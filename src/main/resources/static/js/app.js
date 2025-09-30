document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const promptInput = document.getElementById('prompt-input');
    const sendButton = document.getElementById('send-button');
    const messagesContainer = document.getElementById('messages');
    const statusElement = document.getElementById('status');
    const modelSelector = document.getElementById('model-selector');
    const currentModelElement = document.getElementById('current-model');
    const modelInfoElement = document.getElementById('model-info');

    // Configuration
    const API_ENDPOINT = '/api/ai/ask';
    const USER_NAME = 'You';
    const AI_NAME = 'Ollama';

    // Load saved model from localStorage
    const savedModel = localStorage.getItem('selectedModel');
    if (savedModel) {
        modelSelector.value = savedModel;
        updateModelDisplay(savedModel);
    }

    // Update model display
    function updateModelDisplay(model) {
        currentModelElement.textContent = model;
        modelInfoElement.textContent = `Model: ${model}`;
    }

    // Add a new message to the chat
    function addMessage(role, content) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;
        
        const senderSpan = document.createElement('div');
        senderSpan.className = 'sender';
        senderSpan.textContent = role === 'user' ? USER_NAME : AI_NAME;
        
        const contentDiv = document.createElement('div');
        contentDiv.className = 'content';
        contentDiv.textContent = content;
        
        messageDiv.appendChild(senderSpan);
        messageDiv.appendChild(contentDiv);
        
        // Remove typing indicator if it exists
        removeTypingIndicator();
        
        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Update connection status
    function updateConnectionStatus(connected) {
        const statusElement = document.getElementById('connection-status');
        statusElement.textContent = connected ? 'Connected' : 'Disconnected';
        statusElement.className = `status-indicator ${connected ? 'connected' : 'disconnected'}`;
    }

    // Show typing indicator
    function showTypingIndicator() {
        const typingIndicator = document.createElement('div');
        typingIndicator.id = 'typing-indicator';
        typingIndicator.className = 'message ai typing';
        typingIndicator.innerHTML = '<div class="typing-dots"><span>.</span><span>.</span><span>.</span></div>';
        messagesContainer.appendChild(typingIndicator);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Remove typing indicator
    function removeTypingIndicator() {
        const typingIndicator = document.getElementById('typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }

    // Show status message
    function showStatus(message) {
        statusElement.textContent = message;
    }

    // Clear status message
    function clearStatus() {
        statusElement.textContent = '';
    }

    // Send message to the server
    async function sendMessage() {
        const prompt = promptInput.value.trim();
        if (!prompt) return;

        // Add user message to chat
        addMessage('user', prompt);
        
        // Clear input
        promptInput.value = '';
        promptInput.focus();
        
        // Disable input and button while waiting for response
        promptInput.disabled = true;
        sendButton.disabled = true;
        const originalButtonText = sendButton.innerHTML;
        sendButton.innerHTML = '<span class="loading-spinner"></span> Sending...';
        
        // Show typing indicator
        showTypingIndicator();
        
        // Get selected model
        const selectedModel = modelSelector.value;
        
        try {
            // Call your backend API
            const response = await fetch(
                `${API_ENDPOINT}?prompt=${encodeURIComponent(prompt)}&model=${encodeURIComponent(selectedModel)}`,
                {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json',
                    }
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            let responseText = await response.text();
            
            // Try to parse as JSON, if it fails use the raw text
            try {
                const jsonResponse = JSON.parse(responseText);
                responseText = typeof jsonResponse === 'string' 
                    ? jsonResponse 
                    : jsonResponse.response || jsonResponse.message || JSON.stringify(jsonResponse);
            } catch (e) {
                console.log('Response is not JSON, using raw text');
            }
            
            // Add AI response to chat
            if (responseText) {
                addMessage('ai', responseText);
            } else {
                throw new Error('Empty response from server');
            }
            
            // Update connection status
            updateConnectionStatus(true);
            
        } catch (error) {
            console.error('Error:', error);
            let errorMessage = `Sorry, I encountered an error: ${error.message}`;
            
            if (error.message.includes('model not found')) {
                errorMessage = `Error: The selected model is not available. Please try another model.`;
            } else if (error.message.includes('timeout')) {
                errorMessage = `Error: The request timed out. The model might be loading. Please try again in a moment.`;
            }
            
            addMessage('ai', errorMessage);
            updateConnectionStatus(false);
        } finally {
            // Re-enable input and button
            promptInput.disabled = false;
            sendButton.disabled = false;
            sendButton.innerHTML = originalButtonText;
            clearStatus();
            removeTypingIndicator();
        }
    }

    // Event Listeners
    sendButton.addEventListener('click', sendMessage);
    
    // Send message on Enter (Shift+Enter for new line)
    promptInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    
    // Model selector change
    modelSelector.addEventListener('change', function() {
        const selectedModel = this.value;
        localStorage.setItem('selectedModel', selectedModel);
        updateModelDisplay(selectedModel);
    });
    
    // Auto-resize textarea as user types
    promptInput.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });
    
    // Initial focus on input
    promptInput.focus();
    
    // Add welcome message
    addMessage('ai', `Hello! I'm Ollama. How can I assist you today? (Using model: ${modelSelector.value})`);
    
    // Initial connection status
    updateConnectionStatus(true);
});
