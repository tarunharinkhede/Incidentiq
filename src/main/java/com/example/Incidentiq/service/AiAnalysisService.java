package com.example.Incidentiq.service;

import com.example.Incidentiq.dto.AiAnalysisResult;

import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
@Service
public class AiAnalysisService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a DevOps / Site Reliability Engineer reviewing application logs
                   for an internal incident-management tool. Be precise and concise. Do not invent facts
                        that aren't supported by the log - if something is ambiguous, say so in the summary
                        rather than guessing wildly.
            """;


    private static final String USER_PROMPT_TEMPLATE = """
             Analyze the following application log from service "%s" and return a structured
                        assessment.
            
                        - summary: one or two sentence plain-English description of what went wrong
                        - severity: one of LOW, MEDIUM, HIGH, CRITICAL
                        - category: a short label such as Database, Network, Memory, Configuration, Authentication, Deployment, Third-Party
                        - rootCause: your best-supported hypothesis for the underlying cause
                        - suggestedFix: a concrete, actionable fix
                        - checklist: 3-5 short verification steps an engineer should run through
            
                        LOG:
                        ---
                        %s
                        ---
            """;

    public AiAnalysisService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AiAnalysisResult analyze(String serviceName, String log){

        String userPrompt = USER_PROMPT_TEMPLATE.formatted(serviceName, truncate(log, 8000));
  return chatClient.prompt()
          .system(SYSTEM_PROMPT)
          .user(userPrompt)
          .call()
          .entity(AiAnalysisResult.class);
    }

    private String truncate(String text, int maxChars){
        if(text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "\n...[truncated]";
    }

}