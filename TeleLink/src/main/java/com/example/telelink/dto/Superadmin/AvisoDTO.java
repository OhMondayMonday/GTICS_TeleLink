package com.example.telelink.dto.Superadmin;

import com.example.telelink.entity.Aviso;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Getter
@Setter
public class AvisoDTO {

    private Integer avisoId;
    private String tituloAviso;
    private String textoAviso;
    private String fotoAvisoUrl;
    private LocalDateTime fechaAviso;
    private String estadoAviso;


    // Constructor
    public AvisoDTO(Integer avisoId, String tituloAviso, String textoAviso, String fotoAvisoUrl, LocalDateTime fechaAviso, String estadoAviso) {
        this.avisoId = avisoId;
        this.tituloAviso = tituloAviso;
        this.textoAviso = textoAviso;
        this.fotoAvisoUrl = fotoAvisoUrl;
        this.fechaAviso = fechaAviso;
        this.estadoAviso = estadoAviso;
    }
}
