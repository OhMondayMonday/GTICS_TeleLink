package com.example.telelink.service;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.repository.EspacioDeportivoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class EspacioDeportivoService {

    @Autowired
    private EspacioDeportivoRepository espacioDeportivoRepository;

    public List<EspacioDeportivo> findAll() {
        return espacioDeportivoRepository.findAll();
    }

    public Optional<EspacioDeportivo> findById(Integer id) {
        return espacioDeportivoRepository.findById(id);
    }

    public List<EspacioDeportivo> findByServicioDeportivoId(Integer servicioDeportivoId) {
        return espacioDeportivoRepository.findByServicioDeportivoServicioDeportivoId(servicioDeportivoId);
    }

    public List<EspacioDeportivo> findByEstadoServicio(EspacioDeportivo.EstadoServicio estadoServicio) {
        return espacioDeportivoRepository.findByEstadoServicio(estadoServicio);
    }

    // Método para vecino-index
    public List<EspacioDeportivo> listarEspaciosOperativos() {
        // Convierte el String "operativo" al valor correspondiente del enum EstadoServicio
        EspacioDeportivo.EstadoServicio estadoOperativo = EspacioDeportivo.EstadoServicio.valueOf("operativo");

        // Llama al repositorio pasando el enum
        return espacioDeportivoRepository.findByEstadoServicioOrderByNombreAsc(estadoOperativo);
    }

    /*public Page<EspacioDeportivo> buscarCanchasFiltradas(String tipo, Double precioMax, String zona, Integer rating, Pageable pageable) {
        return espacioDeportivoRepository.findByFilters(tipo, precioMax, zona, rating, pageable);
    }*/
    public Page<EspacioDeportivo> buscarTodasLasCanchas(Pageable pageable) {
        return espacioDeportivoRepository.findAll(pageable);
    }
}

