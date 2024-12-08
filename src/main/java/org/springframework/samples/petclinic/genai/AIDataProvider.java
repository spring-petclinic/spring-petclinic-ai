/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.genai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Functions that are invoked by the LLM will use this bean to query the system of record
 * for information such as listing owners and vets, or adding pets to an owner.
 *
 * @author Oded Shopen
 */
@Service
public class AIDataProvider {

	private final OwnerRepository ownerRepository;

	private final VectorStore vectorStore;

	public AIDataProvider(OwnerRepository ownerRepository, VectorStore vectorStore) {
		this.ownerRepository = ownerRepository;
		this.vectorStore = vectorStore;
	}

	public OwnersResponse getAllOwners() {
		Pageable pageable = PageRequest.of(0, 100);
		Page<Owner> ownerPage = ownerRepository.findAll(pageable);
		return new OwnersResponse(ownerPage.getContent());
	}

	public VetResponse getVets(VetRequest request) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String vetAsJson = objectMapper.writeValueAsString(request.vet());

		SearchRequest sr = SearchRequest.from(SearchRequest.defaults()).withQuery(vetAsJson).withTopK(20);
		if (request.vet() == null) {
			// Provide a limit of 50 results when zero parameters are sent
			sr = sr.withTopK(50);
		}

		List<Document> topMatches = this.vectorStore.similaritySearch(sr);
		List<String> results = topMatches.stream().map(Document::getContent).toList();
		return new VetResponse(results);
	}

	public AddedPetResponse addPetToOwner(AddPetRequest request) {
		var ownerWithPet = ownerRepository.findById(request.ownerId()).map(existingOwner -> {
			existingOwner.addPet(request.pet());
			return ownerRepository.save(existingOwner);
		}).orElse(null);
		return new AddedPetResponse(ownerWithPet);
	}

	public OwnerResponse addOwnerToPetclinic(OwnerRequest ownerRequest) {
		ownerRepository.save(ownerRequest.owner());
		return new OwnerResponse(ownerRequest.owner());
	}

}
