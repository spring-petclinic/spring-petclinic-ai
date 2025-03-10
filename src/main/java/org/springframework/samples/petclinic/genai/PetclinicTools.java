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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class defines the tools (also known as function) that the LLM provider will invoke
 * when it requires more Information on a given topic. The currently available tools
 * enable the LLM to get the list of owners and their pets, get information about the
 * veterinarians, and add a pet to an owner.
 *
 * @author Oded Shopen
 * @author Antoine Rey
 */
@Component
class PetclinicTools {

	private final static Logger LOGGER = LoggerFactory.getLogger(PetclinicTools.class);

	private final AIDataProvider petclinicAiProvider;

	PetclinicTools(AIDataProvider petclinicAiProvider) {
		this.petclinicAiProvider = petclinicAiProvider;
	}

	@Tool(description = "List the owners that the pet clinic has")
	public List<Owner> listOwners() {
		return petclinicAiProvider.getAllOwners();
	}

	@Tool(description = "List the veterinarians that the pet clinic has")
	public List<String> listVets(Vet vet) {
		try {
			return petclinicAiProvider.getVets(vet);
		}
		catch (JsonProcessingException e) {
			LOGGER.error("Listing Veterinarians failed", e);
			return null;
		}
	}

	@Tool(description = ("""
			Add a pet with the specified petTypeId, to an owner identified by the ownerId. \
			The allowed Pet types IDs are only: \
			1 - cat \
			2 - dog \
			3 - lizard \
			4 - snake \
			5 - bird \
			6 - hamster"""))
	public Owner addPetToOwner(Pet pet, Integer ownerId) {
		return petclinicAiProvider.addPetToOwner(ownerId, pet);
	}

	@Tool(description = ("""
			Add a new pet owner to the pet clinic. \
			The Owner must include a first name and a last name as two separate words, \
			plus an address and a 10-digit phone number"""))
	public Owner addOwnerToPetclinic(Owner owner) {
		return petclinicAiProvider.addOwnerToPetclinic(owner);
	}

}
