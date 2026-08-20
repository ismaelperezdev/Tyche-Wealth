package com.tychewealth.service.assetvariation;

import static com.tychewealth.testdata.AssetVariationTestData.OCCURRED_AT;
import static com.tychewealth.testdata.AssetVariationTestData.asset;
import static com.tychewealth.testdata.AssetVariationTestData.variationDto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.asset.AssetVariationDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.enums.AssetVariationTypeEnum;
import com.tychewealth.mapper.assetvariation.AssetVariationMapper;
import com.tychewealth.repository.AssetVariationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetVariationServiceTest {

  @Mock private AssetVariationRepository assetVariationRepository;
  @Mock private AssetVariationMapper assetVariationMapper;
  @InjectMocks private AssetVariationService assetVariationService;

  @Test
  void createMapsAndSavesCreatedVariation() {
    AssetEntity asset = asset(1L, "10.00000000", "150.0000");
    AssetVariationEntity variation = mock(AssetVariationEntity.class);
    AssetVariationDto variationDto =
        variationDto(1L, AssetVariationTypeEnum.CREATED, null, "10.00000000", null, "150.0000");
    when(assetVariationMapper.toEntityForCreation(any(AssetVariationDto.class), eq(asset)))
        .thenReturn(variation);

    assetVariationService.create(asset, OCCURRED_AT);

    ArgumentCaptor<AssetVariationDto> captor = ArgumentCaptor.forClass(AssetVariationDto.class);
    verify(assetVariationMapper).toEntityForCreation(captor.capture(), eq(asset));
    assertVariationEquals(variationDto, captor.getValue());
    verify(assetVariationRepository).save(variation);
  }

  @Test
  void updateMapsPreviousAndNewValues() {
    AssetEntity asset = asset(1L, "12.00000000", "175.0000");
    AssetVariationEntity variation = mock(AssetVariationEntity.class);
    AssetVariationDto variationDto =
        variationDto(
            1L,
            AssetVariationTypeEnum.MANUAL_UPDATE,
            "10.00000000",
            "12.00000000",
            "150.0000",
            "175.0000");
    when(assetVariationMapper.toEntityForCreation(any(AssetVariationDto.class), eq(asset)))
        .thenReturn(variation);

    assetVariationService.update(
        asset, new BigDecimal("10.00000000"), new BigDecimal("150.0000"), OCCURRED_AT);

    ArgumentCaptor<AssetVariationDto> captor = ArgumentCaptor.forClass(AssetVariationDto.class);
    verify(assetVariationMapper).toEntityForCreation(captor.capture(), eq(asset));
    assertVariationEquals(variationDto, captor.getValue());
    verify(assetVariationRepository).save(variation);
  }

  @Test
  void deleteMapsCurrentValuesAsPreviousValues() {
    AssetEntity asset = asset(1L, "10.00000000", "150.0000");
    AssetVariationEntity variation = mock(AssetVariationEntity.class);
    AssetVariationDto variationDto =
        variationDto(1L, AssetVariationTypeEnum.DELETED, "10.00000000", null, "150.0000", null);
    when(assetVariationMapper.toEntityForCreation(any(AssetVariationDto.class), eq(asset)))
        .thenReturn(variation);

    assetVariationService.delete(asset, OCCURRED_AT);

    ArgumentCaptor<AssetVariationDto> captor = ArgumentCaptor.forClass(AssetVariationDto.class);
    verify(assetVariationMapper).toEntityForCreation(captor.capture(), eq(asset));
    assertVariationEquals(variationDto, captor.getValue());
    verify(assetVariationRepository).save(variation);
  }

  @Test
  void createAllMapsAndSavesAllVariations() {
    AssetEntity firstAsset = asset(1L, "10.00000000", "150.0000");
    AssetEntity secondAsset = asset(2L, "5.00000000", "200.0000");
    AssetVariationEntity firstVariation = mock(AssetVariationEntity.class);
    AssetVariationEntity secondVariation = mock(AssetVariationEntity.class);
    when(assetVariationMapper.toEntityForCreation(any(AssetVariationDto.class), eq(firstAsset)))
        .thenReturn(firstVariation);
    when(assetVariationMapper.toEntityForCreation(any(AssetVariationDto.class), eq(secondAsset)))
        .thenReturn(secondVariation);

    assetVariationService.createAll(List.of(firstAsset, secondAsset), OCCURRED_AT);

    verify(assetVariationRepository).saveAll(List.of(firstVariation, secondVariation));
  }

  @Test
  void deleteAllMapsAndSavesAllVariations() {
    AssetEntity firstAsset = asset(1L, "10.00000000", "150.0000");
    AssetEntity secondAsset = asset(2L, "5.00000000", "200.0000");
    AssetVariationEntity firstVariation = mock(AssetVariationEntity.class);
    AssetVariationEntity secondVariation = mock(AssetVariationEntity.class);
    when(assetVariationMapper.toEntityForCreation(any(AssetVariationDto.class), eq(firstAsset)))
        .thenReturn(firstVariation);
    when(assetVariationMapper.toEntityForCreation(any(AssetVariationDto.class), eq(secondAsset)))
        .thenReturn(secondVariation);

    assetVariationService.deleteAll(List.of(firstAsset, secondAsset), OCCURRED_AT);

    verify(assetVariationRepository).saveAll(List.of(firstVariation, secondVariation));
  }

  @Test
  void batchMethodsSkipNullAndEmptyCollections() {
    assetVariationService.createAll(null, OCCURRED_AT);
    assetVariationService.createAll(List.of(), OCCURRED_AT);
    assetVariationService.deleteAll(null, OCCURRED_AT);
    assetVariationService.deleteAll(List.of(), OCCURRED_AT);

    verify(assetVariationMapper, never()).toEntityForCreation(any(), any());
    verify(assetVariationRepository, never()).save(any(AssetVariationEntity.class));
    verify(assetVariationRepository, never()).saveAll(any());
  }

  private void assertVariationEquals(AssetVariationDto expected, AssetVariationDto actual) {
    assertEquals(expected.getAssetId(), actual.getAssetId());
    assertEquals(expected.getChangeType(), actual.getChangeType());
    assertEquals(expected.getPreviousQuantity(), actual.getPreviousQuantity());
    assertEquals(expected.getNewQuantity(), actual.getNewQuantity());
    assertEquals(expected.getPreviousAveragePrice(), actual.getPreviousAveragePrice());
    assertEquals(expected.getNewAveragePrice(), actual.getNewAveragePrice());
    assertEquals(expected.getOccurredAt(), actual.getOccurredAt());
  }
}
