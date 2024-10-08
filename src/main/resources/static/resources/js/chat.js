<!-- JavaScript for handling chatbox interaction -->
<!-- Author : Odedia Shopen -->
function appendMessage(message, type) {
    const chatMessages = document.getElementById('chatbox-messages');
    const messageElement = document.createElement('div');
    messageElement.classList.add('chat-bubble', type);

    // Convert Markdown to HTML
    // May interpret bullet syntax like
    // 1. **Betty Davis**
    const htmlContent = marked.parse(message);
    messageElement.innerHTML = htmlContent;

    chatMessages.appendChild(messageElement);

    // Scroll to the bottom of the chatbox to show the latest message
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function toggleChatbox() {
    const chatbox = document.getElementById('chatbox');
    const chatboxContent = document.getElementById('chatbox-content');

    if (chatbox.classList.contains('minimized')) {
        chatbox.classList.remove('minimized');
        chatboxContent.style.height = '400px'; // Set to initial height when expanded
    } else {
        chatbox.classList.add('minimized');
        chatboxContent.style.height = '40px'; // Set to minimized height
    }
}

function sendMessage() {
    const query = document.getElementById('chatbox-input').value;

    // Only send if there's a message
    if (!query.trim()) return;

    // Clear the input field after sending the message
    document.getElementById('chatbox-input').value = '';

    // Display user message in the chatbox
    appendMessage(query, 'user');

    // Send the message to the backend
    fetch('/chat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(query),
    })
        .then(response => response.text())
        .then(responseText => {
            // Display the response from the server in the chatbox
            appendMessage(responseText, 'bot');
        })
        .catch(error => console.error('Error:', error));
}

function handleKeyPress(event) {
    if (event.key === "Enter") {
        event.preventDefault(); // Prevents adding a newline
        sendMessage(); // Send the message when Enter is pressed
    }
}

// Save chat messages to sessionStorage
function saveChatMessages() {
    const messages = document.getElementById('chatbox-messages').innerHTML;
    sessionStorage.setItem('chatMessages', messages);
}

// Load chat messages from sessionStorage
function loadChatMessages() {
    const messages = sessionStorage.getItem('chatMessages');
    if (messages) {
        document.getElementById('chatbox-messages').innerHTML = messages;
        document.getElementById('chatbox-messages').scrollTop = document.getElementById('chatbox-messages').scrollHeight;
    }
}
