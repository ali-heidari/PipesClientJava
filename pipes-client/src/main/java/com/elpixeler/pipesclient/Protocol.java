package com.elpixeler.pipesclient;

import java.util.Map;

public class Protocol {
    String receiverId, operation, senderId;
    Map<String, Object> input;
    Object res;
    boolean awaiting;

    /**
     * Constructor
     * 
     * @param {String}  receiverId Id of service whose message sent for
     * @param {int}     operation operation id which receiver knows
     * @param {Object}  input Optional - object receiver needs for operation
     * @param {boolean} awaiting Determine if sender is waiting for answer. Default
     *                  is false.
     */
    Protocol(String receiverId, String operation,String senderId,Object result, Map<String, Object> input, boolean awaiting) {
        this.receiverId = receiverId;
        this.operation = operation;
        this.input = input;
        this.awaiting = awaiting;
        this.senderId = senderId;
        this.res = result;
    }
}