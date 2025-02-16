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
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Functions that are invoked by the LLM will use this bean to query the system of record
 * for information such as listing owners and vets, or adding pets to an owner.
 *
 * @author Oded Shopen
 * @author Antoine Rey
 */
@Service
public class AIDataProvider {

	private final OwnerRepository ownerRepository;

	private final VectorStore vectorStore;

	public AIDataProvider(OwnerRepository ownerRepository, VectorStore vectorStore) {
		this.ownerRepository = ownerRepository;
		this.vectorStore = vectorStore;
	}

	public List<Owner> getAllOwners() {
		Pageable pageable = PageRequest.of(0, 100);
		Page<Owner> ownerPage = ownerRepository.findAll(pageable);
		return ownerPage.getContent();
	}

	public List<String> getVets(Vet vet) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String vetAsJson = objectMapper.writeValueAsString(vet);

		// Provide a limit of 50 results when zero parameters are sent
		int topK = (vet == null) ? 50 : 20;
		SearchRequest sr = SearchRequest.builder().query(vetAsJson).topK(topK).build();

		List<Document> topMatches = this.vectorStore.similaritySearch(sr);
		return topMatches.stream().map(Document::getText).toList();
	}

	public Owner addPetToOwner(int ownerId, Pet pet) {
		pet.setId(null); // Non persistent Pet
		return ownerRepository.findById(ownerId).map(existingOwner -> {
			existingOwner.addPet(pet);
			return ownerRepository.save(existingOwner);
		}).orElse(null);
	}

	public Owner addOwnerToPetclinic(Owner owner) {
		return ownerRepository.save(owner);
	}

}
