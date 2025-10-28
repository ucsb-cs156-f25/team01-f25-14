package edu.ucsb.cs156.example.entities;

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
  private String teamId; // team identifier
  private String tableOrBreakoutRoom; // the content of the request
  private LocalDateTime requestTime; // get time
  private String explanation; // optional additional context
  private boolean solved; // could be "true" or "false"
}
