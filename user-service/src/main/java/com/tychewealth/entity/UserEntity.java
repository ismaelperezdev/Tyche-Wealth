package com.tychewealth.entity;

import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_254_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_A_VALID_EMAIL_ADDRESS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_30_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class UserEntity {

  @Id
  @SequenceGenerator(name = "users_seq_gen", sequenceName = "users_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq_gen")
  private Long id;

  @Column(name = "email", nullable = false, length = 254, unique = true)
  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Email(message = MUST_BE_A_VALID_EMAIL_ADDRESS)
  @Size(max = 254, message = MUST_BE_AT_MOST_254_CHARACTERS)
  private String email;

  @Column(name = "username", nullable = false, length = 30, unique = true)
  @NotBlank(message = MUST_NOT_BE_BLANK)
  @Size(min = 3, max = 30, message = MUST_BE_BETWEEN_3_AND_30_CHARACTERS)
  private String username;

  @Column(name = "password", nullable = false, length = 72)
  @NotBlank(message = MUST_NOT_BE_BLANK)
  private String password;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "user")
  private List<PortfolioEntity> portfolios = new ArrayList<>();

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
