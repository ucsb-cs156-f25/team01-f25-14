package edu.ucsb.cs156.team01.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "helprequests")
public class HelpRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private String requesterEmail; // email of the student making the request
  private String teamName; // team identifier
  private String requestText; // the content of the request
  private boolean solved; // could be "true" or "false"
  private String explanation; // optional additional context
  private LocalDateTime requestTime; // get time
}
