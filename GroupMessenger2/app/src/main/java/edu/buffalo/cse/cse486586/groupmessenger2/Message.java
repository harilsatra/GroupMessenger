package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by Haril Satra on 3/13/2017.
 */

public class Message implements Serializable {
    String msg;
    String type;
    int sender_port;
    double suggested_seq;
    int suggester_port;
    boolean deliverable;
    int count;
    double max_agreed;
    int failed_port;
}
