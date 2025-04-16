package pe.edu.vallegrande.report_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.report_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_service.service.ReportService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    /**
     * 🔹 Listado con filtros por query params
     */
    @GetMapping
    public Flux<ReportWithWorkshopsDto> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trimester,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd
    ) {
        return service.findFilteredReports(status, trimester, year, dateStart, dateEnd);
    }

    /**
     * 🔹 Obtener reporte por ID con filtro de fechas
     */
    @GetMapping("/{id}/filtered")
    public Mono<ReportWithWorkshopsDto> getById(
            @PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd
    ) {
        return service.findByIdWithDateFilter(id, dateStart, dateEnd);
    }


    /**
     * 🔹 Crear reporte
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReportWithWorkshopsDto> create(@RequestBody ReportWithWorkshopsDto dto) {
        return service.create(dto);
    }

    /**
     * 🔹 Editar reporte
     */
    @PutMapping("/{id}")
    public Mono<ReportWithWorkshopsDto> update(@PathVariable Integer id, @RequestBody ReportWithWorkshopsDto dto) {
        return service.update(id, dto);
    }

    /**
     * 🔹 Restaurar reporte
     */
    @PutMapping("/restore/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> restore(@PathVariable Integer id) {
        return service.restore(id);
    }

    /**
     * 🔹 Eliminación lógica
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> disable(@PathVariable Integer id) {
        return service.deleteLogic(id);
    }
}
