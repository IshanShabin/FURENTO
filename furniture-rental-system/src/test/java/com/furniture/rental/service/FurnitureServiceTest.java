package com.furniture.rental.service;

import com.furniture.rental.dto.FurnitureFilter;
import com.furniture.rental.repository.FurnitureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FurnitureServiceTest {

    @Mock
    private FurnitureRepository furnitureRepository;

    @InjectMocks
    private FurnitureService furnitureService;

    @Test
    void unknownOrMissingSortFallsBackToNewestFirst() {
        Sort newest = Sort.by(Sort.Direction.DESC, "id");

        assertEquals(newest, FurnitureService.toSort(null));
        assertEquals(newest, FurnitureService.toSort(""));
        assertEquals(newest, FurnitureService.toSort("rentalPricePerMonth; DROP TABLE users"));
    }

    @Test
    void namedSortOptionsMapToTheRightColumns() {
        assertEquals(Sort.by(Sort.Direction.ASC, "rentalPricePerMonth", "id"),
                FurnitureService.toSort("priceAsc"));
        assertEquals(Sort.by(Sort.Direction.DESC, "rentalPricePerMonth", "id"),
                FurnitureService.toSort("priceDesc"));
        assertTrue(FurnitureService.toSort("name").getOrderFor("name").isIgnoreCase());
    }

    @Test
    void likeWildcardsAreEscaped() {
        assertEquals("50!%!_off!!", FurnitureService.escapeLike("50%_off!"));
        assertEquals("plain", FurnitureService.escapeLike("plain"));
    }

    @Test
    void searchPassesPageSizeAndSortToTheRepository() {
        FurnitureFilter filter = new FurnitureFilter();
        filter.setSort("priceAsc");
        filter.setPage(2);
        when(furnitureRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        furnitureService.search(filter, 12);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(furnitureRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(12, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "rentalPricePerMonth", "id"),
                pageable.getValue().getSort());
    }

    @Test
    void missingOrNegativePageMeansTheFirstPage() {
        when(furnitureRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        FurnitureFilter noPage = new FurnitureFilter();
        FurnitureFilter negative = new FurnitureFilter();
        negative.setPage(-5);

        furnitureService.search(noPage, 12);
        furnitureService.search(negative, 12);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(furnitureRepository, org.mockito.Mockito.times(2))
                .findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getAllValues().get(0).getPageNumber());
        assertEquals(0, pageable.getAllValues().get(1).getPageNumber());
    }
}
