package pe.edu.vallegrande.report_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class ReportServiceTest {

    private ReportService reportService;
    private ReportRepository reportRepository;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        reportService = new ReportService(reportRepository, null, null);
    }

    @Test
    void testFindAllReports() {
        Mockito.when(reportRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(reportService.findFilteredReports(null, null, null, null, null))
                .expectNextCount(0)
                .verifyComplete();
    }
}
