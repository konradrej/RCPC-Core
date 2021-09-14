package com.konradrej.rcpc.core.network;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents message for client and server.
 *
 * @author Konrad Rej
 * @author www.konradrej.com
 * @version 1.0
 */
public class Message implements Serializable {
    private final MessageType messageType;
    private final Object messageData;
    private final Map<String, Object> additionalData;

    /**
     * @param messageType the message type
     */
    public Message(MessageType messageType) {
        this(messageType, null, null);
    }

    /**
     * @param messageType the message type
     * @param messageData data required by message type
     */
    public Message(MessageType messageType, Object messageData) {
        this(messageType, messageData, null);
    }

    /**
     * @param messageType    the message type
     * @param messageData    data required by message type
     * @param additionalData any additional data needed
     */
    public Message(MessageType messageType, Object messageData, Map<String, Object> additionalData) {
        this.messageType = messageType;
        this.messageData = messageData;
        this.additionalData = additionalData;
    }

    /**
     * Gets message type.
     *
     * @return this messages type
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Gets message type data.
     *
     * @return this messages type data
     */
    public Object getMessageData() {
        return messageData;
    }

    /**
     * Gets additional data structure.
     *
     * @return this messages additional data structure
     */
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    /**
     * Gets value from additional data structure.
     *
     * @param key key to get from additional data
     * @return this messages value for given key in additional data
     */
    public Object getAdditionalDataFromKey(String key) {
        if (additionalData != null) {
            return additionalData.get(key);
        }

        return null;
    }
}