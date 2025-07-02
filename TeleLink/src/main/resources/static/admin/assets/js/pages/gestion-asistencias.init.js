$(document).ready(function() {
    let asistenciasTable;

    // Inicializar DataTable
    function initDataTable() {
        asistenciasTable = $("#asistenciasTable").DataTable({
            lengthMenu: [5, 10, 25, 50],
            pageLength: 10,
            processing: true,
            serverSide: false,
            ajax: {
                url: '/admin/gestion-asistencias/api/asistencias',
                type: 'GET',
                data: function(d) {
                    d.coordinadorId = $('#filtroCoordinador').val();
                    d.fechaInicio = $('#fechaInicio').val();
                    d.fechaFin = $('#fechaFin').val();
                },
                dataSrc: function(json) {
                    updateExportUrls();
                    return json.data || json;
                }
            },
            columns: [
                {
                    data: null,
                    orderable: true,
                    render: function(data) {
                        return data.coordinador.nombres + ' ' + data.coordinador.apellidos;
                    }
                },
                {
                    data: null,
                    orderable: true,
                    render: function(data) {
                        return data.espacioDeportivo.nombre + '<br><small class="text-muted">' + 
                               data.espacioDeportivo.establecimientoDeportivo.establecimientoDeportivoNombre + '</small>';
                    }
                },
                {
                    data: 'horarioEntrada',
                    orderable: true,
                    render: function(data) {
                        return formatFechaHora(data);
                    }
                },
                {
                    data: 'horarioSalida',
                    orderable: true,
                    render: function(data) {
                        return formatFechaHora(data);
                    }
                },
                {
                    data: null,
                    orderable: true,
                    render: function(data) {
                        return getEstadoBadge(data.estadoEntrada, data.observacionAsistencia);
                    }
                },
                {
                    data: null,
                    orderable: false,
                    render: function(data) {
                        let actions = '';
                        
                        // Botón de reasignar para asistencias canceladas que cumplan la condición
                        // Y que NO hayan sido reasignadas (observacionAsistencia != "reasignada")
                        if (data.estadoEntrada === 'cancelada' && 
                            canReasignar(data.horarioEntrada) && 
                            data.observacionAsistencia !== 'reasignada') {
                            actions = '<button type="button" class="btn btn-danger btn-sm" onclick="mostrarModalReasignar(' + 
                                      data.asistenciaId + ')" title="Reasignar Asistencia">' +
                                      '<i class="fas fa-sync-alt"></i> Reasignar</button>';
                        }
                        // Nuevo botón de reasignar para asistencias pendientes (cancelar y reasignar)
                        else if (data.estadoEntrada === 'pendiente' && canCancelarYReasignar(data.horarioEntrada)) {
                            actions = '<button type="button" class="btn btn-primary btn-sm" onclick="mostrarModalCancelarYReasignar(' + 
                                      data.asistenciaId + ')" title="Cancelar y Reasignar Asistencia">' +
                                      '<i class="fas fa-sync-alt"></i> Reasignar</button>';
                        } else {
                            actions = '<span class="text-muted">-</span>';
                        }
                        
                        return actions;
                    }
                }
            ],
            language: {
                "decimal": "",
                "emptyTable": "No hay asistencias disponibles",
                "info": "Mostrando _START_ a _END_ de _TOTAL_ asistencias",
                "infoEmpty": "Mostrando 0 a 0 de 0 asistencias",
                "infoFiltered": "(filtrado de _MAX_ asistencias totales)",
                "infoPostFix": "",
                "thousands": ",",
                "lengthMenu": "Mostrar _MENU_ asistencias",
                "loadingRecords": "Cargando...",
                "processing": "Procesando...",
                "search": "Buscar:",
                "zeroRecords": "No se encontraron asistencias",
                "paginate": {
                    "first": "Primero",
                    "last": "Último",
                    "next": "<i class='mdi mdi-chevron-right'></i>",
                    "previous": "<i class='mdi mdi-chevron-left'></i>"
                },
                "aria": {
                    "sortAscending": ": activar para ordenar ascendente",
                    "sortDescending": ": activar para ordenar descendente"
                }
            },
            drawCallback: function() {
                $(".dataTables_paginate > .pagination").addClass("pagination-rounded");
            }
        });

        $(".dataTables_length").hide();
    }

    // Formatear fecha y hora
    function formatFechaHora(fechaHora) {
        if (!fechaHora) return '--';
        const fecha = new Date(fechaHora);
        const opciones = {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: true
        };
        return fecha.toLocaleDateString('es-PE', opciones);
    }

    // Obtener badge de estado
    function getEstadoBadge(estado, observacionAsistencia) {
        // Si es cancelada y tiene observación "reasignada", mostrar "Reasignada"
        if (estado === 'cancelada' && observacionAsistencia === 'reasignada') {
            return '<span class="badge bg-purple">Reasignada</span>';
        }
        
        const badges = {
            'pendiente': '<span class="badge bg-info">Pendiente</span>',
            'puntual': '<span class="badge bg-success">Puntual</span>',
            'tarde': '<span class="badge bg-warning">Tardanza</span>',
            'inasistencia': '<span class="badge bg-danger">Inasistencia</span>',
            'cancelada': '<span class="badge bg-secondary">Cancelada</span>'
        };
        return badges[estado] || '<span class="badge bg-light">' + estado + '</span>';
    }

    // Verificar si se puede reasignar (más de 12 horas desde ahora)
    function canReasignar(horarioEntrada) {
        if (!horarioEntrada) return false;
        const fechaAsistencia = new Date(horarioEntrada);
        const ahora = new Date();
        const diferenciasHoras = (fechaAsistencia.getTime() - ahora.getTime()) / (1000 * 60 * 60);
        return diferenciasHoras > 12;
    }

    // Verificar si se puede cancelar y reasignar (más de 24 horas desde ahora)
    function canCancelarYReasignar(horarioEntrada) {
        if (!horarioEntrada) return false;
        const fechaAsistencia = new Date(horarioEntrada);
        const ahora = new Date();
        const diferenciasHoras = (fechaAsistencia.getTime() - ahora.getTime()) / (1000 * 60 * 60);
        return diferenciasHoras > 24;
    }

    // Actualizar URLs de exportación
    function updateExportUrls() {
        const coordinadorId = $('#filtroCoordinador').val();
        const fechaInicio = $('#fechaInicio').val();
        const fechaFin = $('#fechaFin').val();
        
        let excelUrl = '/admin/gestion-asistencias/export/excel?';
        let pdfUrl = '/admin/gestion-asistencias/export/pdf?';
        
        const params = new URLSearchParams();
        if (coordinadorId) params.append('coordinadorId', coordinadorId);
        if (fechaInicio) params.append('fechaInicio', fechaInicio);
        if (fechaFin) params.append('fechaFin', fechaFin);
        
        const queryString = params.toString();
        
        $('#exportExcel').attr('href', excelUrl + queryString);
        $('#exportPdf').attr('href', pdfUrl + queryString);
    }

    // Mostrar modal de reasignación
    window.mostrarModalReasignar = function(asistenciaId) {
        // Obtener datos de la asistencia
        $.get('/admin/gestion-asistencias/api/asistencia/' + asistenciaId)
            .done(function(asistencia) {
                console.log('Datos de asistencia recibidos:', asistencia); // Debug
                
                $('#asistenciaOriginalId').val(asistencia.asistenciaId);
                $('#coordinadorOriginal').text(asistencia.coordinador.nombres + ' ' + asistencia.coordinador.apellidos);
                $('#espacioDeportivo').text(asistencia.espacioDeportivo.nombre);
                $('#fechaHoraInicio').text(formatFechaHora(asistencia.horarioEntrada));
                $('#fechaHoraFin').text(formatFechaHora(asistencia.horarioSalida));
                
                // Mostrar motivo de cancelación (observaciones de la asistencia)
                console.log('observacionAsistencia:', asistencia.observacionAsistencia);
                let motivoCancelacion = '';
                if (asistencia.observacionAsistencia && asistencia.observacionAsistencia.trim() !== '') {
                    if (asistencia.observacionAsistencia === 'reasignada') {
                        motivoCancelacion = 'Esta asistencia fue reasignada a otro coordinador';
                    } else {
                        motivoCancelacion = asistencia.observacionAsistencia;
                    }
                } else {
                    motivoCancelacion = 'No se especificó motivo';
                }
                console.log('Motivo de cancelación final:', motivoCancelacion); // Debug
                $('#motivoCancelacion').text(motivoCancelacion);
                
                // Configurar modal para reasignación (asistencia ya cancelada)
                $('#modalReasignarLabel').html('<i class="ri-user-settings-line me-2"></i>Reasignar Asistencia');
                $('#btnReasignar').html('<i class="fas fa-user-check me-1"></i>Reasignar Asistencia');
                $('#btnReasignar').removeClass('btn-warning').addClass('btn-primary');
                $('#btnReasignar').off('click').on('click', function() {
                    reasignarAsistencia();
                });
                
                // Mostrar la sección de motivo de cancelación
                $('.card-header h6').html('<i class="fas fa-info-circle me-2"></i>Información de la Asistencia Cancelada');
                $('#motivoCancelacion').parent().show();
                
                // Cargar coordinadores disponibles
                cargarCoordinadoresDisponibles(asistencia.horarioEntrada, asistencia.horarioSalida);
                
                // Mostrar modal
                $('#modalReasignarAsistencia').modal('show');
                
                // Forzar z-index del backdrop después de mostrar el modal
                setTimeout(function() {
                    $('.modal-backdrop').css({
                        'z-index': '99998',
                        'background-color': 'rgba(0, 0, 0, 0.5)'
                    });
                }, 100);
            })
            .fail(function() {
                Swal.fire('Error', 'No se pudo cargar la información de la asistencia', 'error');
            });
    };

    // Mostrar modal de cancelar y reasignar (para asistencias pendientes)
    window.mostrarModalCancelarYReasignar = function(asistenciaId) {
        // Obtener datos de la asistencia
        $.get('/admin/gestion-asistencias/api/asistencia/' + asistenciaId)
            .done(function(asistencia) {
                console.log('Datos de asistencia para cancelar y reasignar:', asistencia);
                
                $('#asistenciaOriginalId').val(asistencia.asistenciaId);
                $('#coordinadorOriginal').text(asistencia.coordinador.nombres + ' ' + asistencia.coordinador.apellidos);
                $('#espacioDeportivo').text(asistencia.espacioDeportivo.nombre);
                $('#fechaHoraInicio').text(formatFechaHora(asistencia.horarioEntrada));
                $('#fechaHoraFin').text(formatFechaHora(asistencia.horarioSalida));
                
                // Configurar modal para cancelar y reasignar
                $('#modalReasignarLabel').html('<i class="fas fa-exchange-alt me-2"></i>Cancelar y Reasignar Asistencia');
                $('#btnReasignar').html('<i class="fas fa-exchange-alt me-1"></i>Cancelar y Reasignar');
                $('#btnReasignar').removeClass('btn-primary').addClass('btn-warning');
                $('#btnReasignar').off('click').on('click', function() {
                    cancelarYReasignarAsistencia();
                });
                
                // Ocultar la sección de motivo de cancelación para asistencias pendientes
                $('.card-header h6').html('<i class="fas fa-info-circle me-2"></i>Información de la Asistencia Actual');
                $('#motivoCancelacion').parent().hide();
                
                // Cargar coordinadores disponibles
                cargarCoordinadoresDisponibles(asistencia.horarioEntrada, asistencia.horarioSalida);
                
                // Mostrar modal
                $('#modalReasignarAsistencia').modal('show');
                
                // Forzar z-index del backdrop después de mostrar el modal
                setTimeout(function() {
                    $('.modal-backdrop').css({
                        'z-index': '99998',
                        'background-color': 'rgba(0, 0, 0, 0.5)'
                    });
                }, 100);
            })
            .fail(function() {
                Swal.fire('Error', 'No se pudo cargar la información de la asistencia', 'error');
            });
    };

    // Cargar coordinadores disponibles
    function cargarCoordinadoresDisponibles(horarioEntrada, horarioSalida) {
        const $select = $('#nuevoCoordinador');
        $select.html('<option value="">Cargando coordinadores...</option>');
        
        $.get('/admin/coordinadores-disponibles', {
            horarioEntrada: horarioEntrada,
            horarioSalida: horarioSalida
        })
        .done(function(coordinadores) {
            $select.html('<option value="">Seleccione un coordinador disponible...</option>');
            coordinadores.forEach(function(coordinador) {
                $select.append('<option value="' + coordinador.usuarioId + '">' + 
                              coordinador.nombres + ' ' + coordinador.apellidos + '</option>');
            });
        })
        .fail(function() {
            $select.html('<option value="">Error al cargar coordinadores</option>');
        });
    }

    // Función para reasignar asistencia ya cancelada
    function reasignarAsistencia() {
        const asistenciaOriginalId = $('#asistenciaOriginalId').val();
        const nuevoCoordinadorId = $('#nuevoCoordinador').val();

        if (!nuevoCoordinadorId) {
            Swal.fire('Error', 'Debe seleccionar un coordinador', 'error');
            return;
        }

        // Mostrar loading
        const $btn = $('#btnReasignar');
        const originalText = $btn.html();
        $btn.html('<i class="fas fa-spinner fa-spin me-1"></i>Reasignando...');
        $btn.prop('disabled', true);

        // Usar fetch API para mayor control
        fetch('/admin/gestion-asistencias/api/reasignar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                'asistenciaOriginalId': asistenciaOriginalId,
                'nuevoCoordinadorId': nuevoCoordinadorId
            })
        })
        .then(response => {
            console.log('Status de respuesta:', response.status);
            if (!response.ok) {
                throw new Error('Error en la respuesta del servidor');
            }
            return response.json();
        })
        .then(data => {
            console.log('Datos recibidos:', data);
            
            // Restaurar botón inmediatamente
            $btn.html(originalText);
            $btn.prop('disabled', false);
            
            if (data.success) {
                // Cerrar modal inmediatamente
                $('#modalReasignarAsistencia').modal('hide');
                
                // Forzar cierre del modal después de 100ms como respaldo
                setTimeout(function() {
                    $('#modalReasignarAsistencia').removeClass('show');
                    $('.modal-backdrop').remove();
                    $('body').removeClass('modal-open');
                }, 100);
                
                // Recargar tabla inmediatamente
                if (asistenciasTable) {
                    asistenciasTable.ajax.reload(null, false); // false = mantener paginación actual
                }
                
                // Mostrar mensaje de éxito después de recargar
                setTimeout(function() {
                    Swal.fire('Éxito', data.message || 'Asistencia reasignada correctamente', 'success');
                }, 200);
                
            } else {
                Swal.fire('Error', data.message || 'Error al reasignar la asistencia', 'error');
            }
        })
        .catch(error => {
            console.error('Error en fetch:', error);
            
            // Restaurar botón
            $btn.html(originalText);
            $btn.prop('disabled', false);
            
            Swal.fire('Error', 'Error al procesar la solicitud', 'error');
        });
    }

    // Función para cancelar y reasignar asistencia pendiente
    function cancelarYReasignarAsistencia() {
        const asistenciaOriginalId = $('#asistenciaOriginalId').val();
        const nuevoCoordinadorId = $('#nuevoCoordinador').val();

        if (!nuevoCoordinadorId) {
            Swal.fire('Error', 'Debe seleccionar un coordinador', 'error');
            return;
        }

        // Mostrar loading
        const $btn = $('#btnReasignar');
        const originalText = $btn.html();
        $btn.html('<i class="fas fa-spinner fa-spin me-1"></i>Procesando...');
        $btn.prop('disabled', true);

        // Usar fetch API para el nuevo endpoint
        fetch('/admin/gestion-asistencias/api/cancelar-y-reasignar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                'asistenciaOriginalId': asistenciaOriginalId,
                'nuevoCoordinadorId': nuevoCoordinadorId
            })
        })
        .then(response => {
            console.log('Status de respuesta:', response.status);
            if (!response.ok) {
                throw new Error('Error en la respuesta del servidor');
            }
            return response.json();
        })
        .then(data => {
            console.log('Datos recibidos:', data);
            
            // Restaurar botón inmediatamente
            $btn.html(originalText);
            $btn.prop('disabled', false);
            
            if (data.success) {
                // Cerrar modal inmediatamente
                $('#modalReasignarAsistencia').modal('hide');
                
                // Forzar cierre del modal después de 100ms como respaldo
                setTimeout(function() {
                    $('#modalReasignarAsistencia').removeClass('show');
                    $('.modal-backdrop').remove();
                    $('body').removeClass('modal-open');
                }, 100);
                
                // Recargar tabla inmediatamente
                if (asistenciasTable) {
                    asistenciasTable.ajax.reload(null, false); // false = mantener paginación actual
                }
                
                // Mostrar mensaje de éxito después de recargar
                setTimeout(function() {
                    Swal.fire('Éxito', data.message || 'Asistencia cancelada y reasignada correctamente', 'success');
                }, 200);
                
            } else {
                Swal.fire('Error', data.message || 'Error al cancelar y reasignar la asistencia', 'error');
            }
        })
        .catch(error => {
            console.error('Error en fetch:', error);
            
            // Restaurar botón
            $btn.html(originalText);
            $btn.prop('disabled', false);
            
            Swal.fire('Error', 'Error al procesar la solicitud', 'error');
        });
    }

    // Manejar filtros
    $('#btnFiltrar').click(function() {
        asistenciasTable.ajax.reload();
    });

    $('#btnLimpiar').click(function() {
        $('#filtroCoordinador').val('');
        $('#fechaInicio').val('');
        $('#fechaFin').val('');
        asistenciasTable.ajax.reload();
    });

    // Limpiar modal al cerrarlo y recargar tabla
    $('#modalReasignarAsistencia').on('hidden.bs.modal', function() {
        $('#formReasignarAsistencia')[0].reset();
        $('#mensajeEstado').addClass('d-none');
        $('#nuevoCoordinador').html('<option value="">Seleccione un coordinador disponible...</option>');
        
        // Recargar tabla como medida de respaldo
        if (asistenciasTable) {
            asistenciasTable.ajax.reload(null, false);
        }
    });

    // Inicializar tabla
    initDataTable();
});
