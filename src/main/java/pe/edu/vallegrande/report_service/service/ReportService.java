package pe.edu.vallegrande.report_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_service.model.Report;
import pe.edu.vallegrande.report_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import pe.edu.vallegrande.report_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepo;
    private final ReportWorkshopRepository workshopRepo;
    private final WorkshopCacheRepository workshopCacheRepo;
    private final SupabaseStorageService storageService;


    /**
     * 🔹 Obtener reportes con talleres filtrados por fecha + otros filtros
     */
    public Flux<ReportWithWorkshopsDto> findFilteredReports(String status, String trimester, Integer year, LocalDate dateStart, LocalDate dateEnd) {
        Flux<Report> baseQuery = (status != null) ? reportRepo.findByStatus(status) : reportRepo.findAll();

        if (trimester != null) {
            baseQuery = baseQuery.filter(r -> trimester.equalsIgnoreCase(r.getTrimester()));
        }
        if (year != null) {
            baseQuery = baseQuery.filter(r -> year.equals(r.getYear()));
        }

        return baseQuery.flatMap(report ->
                workshopRepo.findByReportId(report.getId())
                        .flatMap(rw ->
                                workshopCacheRepo.findById(rw.getWorkshopId())
                                        .filter(wc -> {
                                            boolean inRange = true;
                                            if (dateStart != null) {
                                                inRange = !wc.getDateStart().isBefore(dateStart);
                                            }
                                            if (dateEnd != null) {
                                                inRange = inRange && !wc.getDateEnd().isAfter(dateEnd);
                                            }
                                            return inRange;
                                        })
                                        .map(wc -> {
                                            ReportWorkshopDto dto = toWorkshopDto(rw);
                                            // 👉 Enriquecer DTO con datos del taller original
                                            dto.setWorkshopStatus(wc.getStatus());
                                            dto.setDateStart(wc.getDateStart());
                                            dto.setDateEnd(wc.getDateEnd());
                                            dto.setName(wc.getName());
                                            return dto;
                                        })
                        )
                        .collectList()
                        .filter(list -> !list.isEmpty())
                        .map(workshops -> {
                            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
                            dto.setReport(toDto(report));
                            dto.setWorkshops(workshops);
                            return dto;
                        })
        );
    }

    /**
     * 🔹 Obtener por ID con talleres filtrados por fechas
     */
    public Mono<ReportWithWorkshopsDto> findByIdWithDateFilter(Integer id, LocalDate dateStart, LocalDate dateEnd) {
        Mono<Report> reportMono = reportRepo.findById(id);

        Flux<ReportWorkshopDto> workshopsFlux = workshopRepo.findByReportId(id)
                .flatMap(rw ->
                        workshopCacheRepo.findById(rw.getWorkshopId())
                                .filter(wc -> {
                                    boolean inRange = true;
                                    if (dateStart != null) {
                                        inRange = !wc.getDateStart().isBefore(dateStart);
                                    }
                                    if (dateEnd != null) {
                                        inRange = inRange && !wc.getDateEnd().isAfter(dateEnd);
                                    }
                                    return inRange;
                                })
                                .map(wc -> {
                                    ReportWorkshopDto dto = toWorkshopDto(rw);
                                    // Enriquecer el DTO con datos del cache
                                    dto.setWorkshopStatus(wc.getStatus());
                                    dto.setDateStart(wc.getDateStart());
                                    dto.setDateEnd(wc.getDateEnd());
                                    dto.setName(wc.getName());
                                    return dto;
                                })
                );

        return Mono.zip(reportMono, workshopsFlux.collectList(), (report, workshops) -> {
            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
            dto.setReport(toDto(report));
            dto.setWorkshops(workshops);
            return dto;
        });
    }

    /**
     * 🔹 Crear reporte con talleres
     */
    public Mono<ReportWithWorkshopsDto> create(ReportWithWorkshopsDto dto) {
        Report report = fromDto(dto.getReport());
        report.setStatus("A");

        List<ReportWorkshop> workshops = dto.getWorkshops().stream()
                .map(this::fromWorkshopDto)
                .collect(Collectors.toList());

        String rawSchedule = dto.getReport().getSchedule();

        Mono<String> scheduleMono;
        if (rawSchedule == null) {
            scheduleMono = Mono.just(""); // o Mono.empty() si prefieres
        } else if (isBase64(rawSchedule)) {
            scheduleMono = storageService.uploadBase64Image("reports/schedules", rawSchedule);
        } else {
            scheduleMono = Mono.just(rawSchedule);
        }

        return scheduleMono.flatMap(scheduleUrl -> {
            report.setSchedule(scheduleUrl);

            return reportRepo.save(report)
                    .flatMap(saved -> Flux.fromIterable(workshops)
                            .flatMap(workshop -> Flux.fromIterable(dto.getWorkshops())
                                    .filter(dtoW -> dtoW.getWorkshopId().equals(workshop.getWorkshopId()))
                                    .next()
                                    .flatMapMany(dtoW -> Flux.fromIterable(Arrays.asList(dtoW.getImageUrl()))
                                            .flatMap(image -> isBase64(image)
                                                    ? storageService.uploadBase64Image("reports/workshops", image)
                                                    : Mono.just(image)
                                            )
                                    )
                                    .collectList()
                                    .map(urls -> {
                                        workshop.setReportId(saved.getId());
                                        workshop.setImageUrl(urls.toArray(new String[0]));
                                        return workshop;
                                    })
                            )
                            .collectList()
                            .flatMap(savedWorkshops -> workshopRepo.saveAll(savedWorkshops).collectList())
                            .map(savedWorkshops -> {
                                ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                                response.setReport(toDto(saved));
                                response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).toList());
                                log.info("✅ Reporte creado: {}", response);
                                return response;
                            }));
        });
    }

    /**
     * 🔹 Editar reporte y reemplazar talleres
     */
    public Mono<ReportWithWorkshopsDto> update(Integer id, ReportWithWorkshopsDto dto) {
        return reportRepo.findById(id)
                .flatMap(existing -> {
                    existing.setYear(dto.getReport().getYear());
                    existing.setTrimester(dto.getReport().getTrimester());
                    existing.setDescription(dto.getReport().getDescription());

                    String rawSchedule = dto.getReport().getSchedule();
                    Mono<String> scheduleMono = rawSchedule == null
                            ? Mono.just(existing.getSchedule())
                            : isBase64(rawSchedule)
                            ? storageService.uploadBase64Image("reports/schedules", rawSchedule)
                            : Mono.just(rawSchedule);

                    return scheduleMono.flatMap(scheduleUrl -> {
                        existing.setSchedule(scheduleUrl);

                        return workshopRepo.findByReportId(id)
                                .collectList()
                                .flatMap(oldWorkshops -> {
                                    // Obtener todas las URLs antiguas
                                    List<String> oldUrls = oldWorkshops.stream()
                                            .flatMap(rw -> rw.getImageUrl() == null ?
                                                    Stream.empty() :
                                                    Arrays.stream(rw.getImageUrl()))
                                            .collect(Collectors.toList());

                                    // Obtener nuevas URLs desde el DTO
                                    List<String> newUrls = dto.getWorkshops().stream()
                                            .flatMap(w -> w.getImageUrl() == null ?
                                                    Stream.empty() :
                                                    Arrays.stream(w.getImageUrl()))
                                            .collect(Collectors.toList());

                                    // Determinar cuáles eliminar
                                    List<String> toDelete = oldUrls.stream()
                                            .filter(url -> !newUrls.contains(url))
                                            .collect(Collectors.toList());

                                    // Ejecutar eliminación en Supabase
                                    return Flux.fromIterable(toDelete)
                                            .flatMap(storageService::deleteImage)
                                            .then();
                                })
                                .then(reportRepo.save(existing))
                                .flatMap(saved -> Flux.fromIterable(dto.getWorkshops())
                                        .flatMap(dtoW -> Flux.fromIterable(Arrays.asList(dtoW.getImageUrl()))
                                                .flatMap(image -> isBase64(image)
                                                        ? storageService.uploadBase64Image("reports/workshops", image)
                                                        : Mono.just(image)
                                                )
                                                .collectList()
                                                .map(urls -> {
                                                    ReportWorkshop rw = fromWorkshopDto(dtoW);
                                                    rw.setReportId(saved.getId());
                                                    rw.setId(null); // Forzar INSERT
                                                    rw.setImageUrl(urls.toArray(new String[0]));
                                                    return rw;
                                                })
                                        )
                                        .collectList()
                                        .flatMap(newList -> workshopRepo.deleteByReportId(id)
                                                .then(workshopRepo.saveAll(newList).collectList()))
                                        .map(savedWorkshops -> {
                                            ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                                            response.setReport(toDto(saved));
                                            response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).toList());
                                            log.info("✏️ Reporte actualizado con limpieza de imágenes: {}", response);
                                            return response;
                                        }));
                    });
                });
    }

    /**
     * 🔹 Restaurar reporte (status = A)
     */
    public Mono<Void> restore(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("A");
                    return reportRepo.save(r).then();
                });
    }


    /**
     * 🔹 Eliminación lógica (status = I)
     */
    public Mono<Void> deleteLogic(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("I");
                    return reportRepo.save(r).then();
                });
    }

    // ======================= MAPEO DTO =======================

    private ReportDto toDto(Report r) {
        ReportDto dto = new ReportDto();
        dto.setId(r.getId());
        dto.setYear(r.getYear());
        dto.setTrimester(r.getTrimester());
        dto.setDescription(r.getDescription());
        dto.setSchedule(r.getSchedule());
        dto.setStatus(r.getStatus());
        return dto;
    }

    private Report fromDto(ReportDto dto) {
        return Report.builder()
                .id(dto.getId())
                .year(dto.getYear())
                .trimester(dto.getTrimester())
                .description(dto.getDescription())
                .schedule(dto.getSchedule())
                .status("A") // Siempre lo forzamos al crear
                .build();
    }

    private ReportWorkshopDto toWorkshopDto(ReportWorkshop rw) {
        ReportWorkshopDto dto = new ReportWorkshopDto();
        dto.setId(rw.getId());
        dto.setReportId(rw.getReportId());
        dto.setWorkshopId(rw.getWorkshopId());
        dto.setWorkshopName(rw.getWorkshopName());
        dto.setDescription(rw.getDescription());
        dto.setImageUrl(rw.getImageUrl());
        return dto;
    }

    private ReportWorkshop fromWorkshopDto(ReportWorkshopDto dto) {
        return ReportWorkshop.builder()
                .id(dto.getId())
                .reportId(dto.getReportId())
                .workshopId(dto.getWorkshopId())
                .workshopName(dto.getWorkshopName())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .build();
    }

    private boolean isBase64(String input) {
        return input != null && input.startsWith("data:image/");
    }
}
