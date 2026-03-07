package com.tychewealth.dto.portfolio;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioResponseDto {

    private Long id;
    private String name;
    private String description;
    private String currency;
    private String riskProfile;
    private String investmentHorizon;
    private String strategyType;
    private Double maxRisk;
    private LocalDate startDate;
    private LocalDate updatedAt;
}
