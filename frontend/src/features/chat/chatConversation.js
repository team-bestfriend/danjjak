let savedConversation = null;

export function preserveChatConversation(conversation) {
  savedConversation = {
    messages: conversation.messages.map((message) => ({ ...message })),
    recommendations: conversation.recommendations.map((recommendation) => ({ ...recommendation })),
    nextMessageId: conversation.nextMessageId,
  };
}

export function restoreChatConversation() {
  const conversation = savedConversation;
  savedConversation = null;
  return conversation;
}

export function discardChatConversation() {
  savedConversation = null;
}
