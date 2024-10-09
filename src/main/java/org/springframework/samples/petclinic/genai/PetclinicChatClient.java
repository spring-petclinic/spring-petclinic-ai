package org.springframework.samples.petclinic.genai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * This REST controller is being invoked by the in order to interact with the LLM
 *
 * @author Oded Shopen
 */
@RestController
public class PetclinicChatClient {

	// ChatModel is the primary interfaces for interacting with an LLM
	// it is a request/response interface that implements the ModelModel
	// interface. Make suer to visit the source code of the ChatModel and
	// checkout the interfaces in the core spring ai package.
	private final ChatClient chatClient;

	public PetclinicChatClient(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@PostMapping("/chat")
	public String exchange(@RequestBody String query) {
		// All chatbot messages go through this endpoint and are passed to the LLM
		return this.chatClient.prompt().user(u -> u.text(query)).call().content();
	}

}
