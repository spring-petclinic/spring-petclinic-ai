package org.springframework.samples.petclinic.genai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_CONVERSATION_ID;

@Configuration
class ChatConfiguration {

	@Value("classpath:/prompts/system.st")
	private Resource systemResource;

	@Bean
	ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
		// @formatter:off
		return chatClientBuilder
			.defaultAdvisors(
				// Chat memory helps us keep context when using the chatbot for up to 10 previous messages.
				new MessageChatMemoryAdvisor(chatMemory, DEFAULT_CHAT_MEMORY_CONVERSATION_ID, 10), // CHAT MEMORY
				new SimpleLoggerAdvisor()
			)
			.defaultSystem(systemResource)
			//.defaultFunctions("queryOwners", "addPetToOwner")
			.build();
		// @formatter:on
	}
	@Bean
	public ChatMemory chatMemory() {
		return new InMemoryChatMemory();
	}
}
