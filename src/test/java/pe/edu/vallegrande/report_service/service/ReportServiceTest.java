package pe.edu.vallegrande.report_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import pe.edu.vallegrande.report_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class ReportServiceTest {

    private ReportService reportService;
    private ReportRepository reportRepository;
    private ReportWorkshopRepository reportWorkshopRepository;
    private WorkshopCacheRepository workshopCacheRepository;
    private SupabaseStorageService supabaseStorageService;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        reportWorkshopRepository = Mockito.mock(ReportWorkshopRepository.class);
        workshopCacheRepository = Mockito.mock(WorkshopCacheRepository.class);
        supabaseStorageService = Mockito.mock(SupabaseStorageService.class);

        reportService = new ReportService(
                reportRepository,
                reportWorkshopRepository,
                workshopCacheRepository,
                supabaseStorageService
        );
    }

    @Test
    void testFindAllReports() {
        Mockito.when(reportRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(reportService.findFilteredReports(null, null, null, null, null))
                .expectNextCount(0)
                .verifyComplete();
    }
}
