package com.smart_desk.demo.ai;

import com.smart_desk.demo.entities.Comment;
import com.smart_desk.demo.entities.Ticket;

import java.util.List;

public interface AiProvider {
    CategorizationResult categorize(String title, String description);
    String suggestReply(Ticket ticket, List<Comment> comments);
    String summarizeThread(Ticket ticket, List<Comment> comments);
}
