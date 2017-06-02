package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by Haril Satra on 3/25/2017.
 */
//http://stackoverflow.com/questions/683041/how-do-i-use-a-priorityqueue
public class MessageComparator implements Comparator<Message> {
    @Override
    public int compare(Message x, Message y){
        if (x.max_agreed < y.max_agreed)
        {
            return -1;
        }
        if (x.max_agreed > y.max_agreed)
        {
            return 1;
        }
        return 0;
    }
}
