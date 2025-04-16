package pe.edu.vallegrande.report_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_service.model.WorkshopCache;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 🔹 Servicio para consultar los talleres almacenados en el cache.
 */
@Service
@RequiredArgsConstructor
public class WorkshopCacheService {

    private final WorkshopCacheRepository repository;

    /**
     * 🔸 Lista todos los talleres del cache.
     */
    public Flux<WorkshopCache> findAll() {
        return repository.findAll();
    }

    /**
     * 🔸 Busca un taller del cache por ID.
     */
    public Mono<WorkshopCache> findById(Integer id) {
        return repository.findById(id);
    }
}
