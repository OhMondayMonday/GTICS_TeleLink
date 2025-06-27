// Calendario con carga dinámica y formateo en frontend
!function(v){
    "use strict";

    function e(){}

    e.prototype.init=function(){
        var espacioId = document.getElementById("espacio-id").value;
        var calendar = new FullCalendar.Calendar(document.getElementById("calendar"), {
            plugins: ["bootstrap", "interaction", "dayGrid", "timeGrid"],
            editable: false,
            droppable: false,
            selectable: false,
            defaultView: "timeGridWeek",
            themeSystem: "bootstrap",
            allDaySlot: false,
            locale: 'es',
            slotMinTime: "08:00:00",
            slotMaxTime: "22:00:00",
            scrollTime:"08:00:00",
            header: {
                left: "prev,next today",
                center: "title",
                right: "dayGridMonth,timeGridWeek,timeGridDay"
            },
            buttonText: {
                today: 'Hoy',
                month: 'Mes',
                week: 'Semana',
                day: 'Día'
            },
            eventTimeFormat: {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            },
            // Evitar que los eventos se solapen
            slotEventOverlap: false,
            // Limitar eventos por celda y apilar en columnas
            dayMaxEvents: true,
            eventMaxStack: 2,
            // Tooltip personalizado con texto completo
            eventDidMount: function(info) {
                // Crear tooltip con el título completo y detalles adicionales
                let tooltipContent = info.event.title;
                
                // Añadir detalles según el tipo de evento
                if (info.event.extendedProps.tipo === 'reserva') {
                    tooltipContent += `\nHorario: ${info.event.extendedProps.horario}`;
                } else if (info.event.extendedProps.tipo === 'asistencia') {
                    tooltipContent += `\nCoordinador: ${info.event.extendedProps.coordinador}`;
                    tooltipContent += `\nEstado: ${info.event.extendedProps.estado}`;
                }

                new bootstrap.Tooltip(info.el, {
                    title: tooltipContent,
                    placement: 'top',
                    trigger: 'hover',
                    container: 'body',
                    html: true,
                    template: '<div class="tooltip" role="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner text-start text-wrap" style="max-width: 300px;"></div></div>'
                });
            },
            events: function(fetchInfo, successCallback, failureCallback) {
                Promise.all([
                    // Obtener reservas consolidadas
                    fetch(`/admin/calendario/reservas/${espacioId}?start=${fetchInfo.start.toISOString()}&end=${fetchInfo.end.toISOString()}`)
                        .then(response => response.json()),
                    // Obtener asistencias
                    fetch(`/admin/calendario/asistencias/${espacioId}?start=${fetchInfo.start.toISOString()}&end=${fetchInfo.end.toISOString()}`)
                        .then(response => response.json())
                ])
                .then(([reservas, asistencias]) => {
                    console.log('Reservas:', reservas);
                    console.log('Asistencias:', asistencias);                    const eventos = [
                        ...reservas.map(reserva => ({
                            title: `Reservado (${reserva.horario})`,
                            start: reserva.inicio,
                            end: reserva.fin,
                            backgroundColor: reserva.tieneAsistenciaSolapada ? '#6c757d' : '#556ee6', // Gris si tiene asistencia, azul si no
                            borderColor: reserva.tieneAsistenciaSolapada ? '#6c757d' : '#556ee6',
                            extendedProps: {
                                tipo: 'reserva',
                                horario: reserva.horario,
                                tieneAsistenciaSolapada: reserva.tieneAsistenciaSolapada,
                                espacioDeportivoId: reserva.espacioDeportivoId,
                                espacioDeportivoNombre: reserva.espacioDeportivoNombre
                            }
                        })),
                        ...asistencias.map(asistencia => ({
                            title: `${asistencia.coordinador}`,
                            start: asistencia.inicio,
                            end: asistencia.fin,
                            backgroundColor: '#50a5f1', // info
                            borderColor: '#50a5f1',
                            extendedProps: {
                                tipo: 'asistencia',
                                coordinador: asistencia.coordinador,
                                estado: asistencia.estado
                            }
                        }))
                    ];

                    successCallback(eventos);
                })
                .catch(error => {
                    console.error('Error al obtener eventos:', error);
                    failureCallback(error);
                });
            },            eventClick: function(info) {
                info.jsEvent.preventDefault();

                const event = info.event;
                const tipo = event.extendedProps.tipo;

                if (tipo === 'reserva' && !event.extendedProps.tieneAsistenciaSolapada) {
                    // Verificar si la reserva inicia en más de 24 horas desde ahora
                    const ahora = new Date();
                    const inicioReserva = new Date(event.start);
                    const diferenciaMilisegundos = inicioReserva.getTime() - ahora.getTime();
                    const diferenciaHoras = diferenciaMilisegundos / (1000 * 60 * 60); // Convertir a horas

                    if (diferenciaHoras > 24) {
                        // Solo mostrar modal si la reserva inicia en más de 24 horas
                        mostrarModalCrearAsistencia(event);
                    }
                    // Si no cumple la condición de 24 horas, no hacer nada
                } else {
                    // Mostrar información del evento
                    let content = `<div class="p-3">`;
                    if (tipo === 'reserva') {
                        content += `
                            <h5>Reserva</h5>
                            <hr>
                            <p><strong>Horario:</strong> ${event.extendedProps.horario}</p>
                            ${event.extendedProps.tieneAsistenciaSolapada ? 
                                '<p class="text-muted"><i class="mdi mdi-information-outline"></i> Ya tiene asistencia asignada</p>' : ''}
                        `;
                    } else {
                        content += `
                            <h5>Asistencia</h5>
                            <hr>
                            <p><strong>Coordinador:</strong> ${event.extendedProps.coordinador}</p>
                            <p><strong>Estado:</strong> ${event.extendedProps.estado}</p>
                            <p><strong>Horario:</strong> ${event.start.toLocaleTimeString('es-PE')} - ${event.end.toLocaleTimeString('es-PE')}</p>
                        `;
                    }
                    content += '</div>';

                    Swal.fire({
                        title: 'Detalles del Evento',
                        html: content,
                        confirmButtonText: 'Cerrar',
                        customClass: {
                            popup: 'text-left'
                        }
                    });
                }
            }
        });

        calendar.render();        // Actualizar cada 5 minutos
        setInterval(function() {
            console.log('Actualizando calendario...');
            calendar.refetchEvents();
        }, 300000);
    },

    // Función para mostrar el modal de crear asistencia
    window.mostrarModalCrearAsistencia = function(event) {
        const espacioId = event.extendedProps.espacioDeportivoId;
        const espacioNombre = event.extendedProps.espacioDeportivoNombre;
        const inicio = event.start;
        const fin = event.end;

        // Formatear fechas para input datetime-local
        const formatoDatetimeLocal = (fecha) => {
            const year = fecha.getFullYear();
            const month = String(fecha.getMonth() + 1).padStart(2, '0');
            const day = String(fecha.getDate()).padStart(2, '0');
            const hours = String(fecha.getHours()).padStart(2, '0');
            const minutes = String(fecha.getMinutes()).padStart(2, '0');
            return `${year}-${month}-${day}T${hours}:${minutes}`;
        };

        // Llenar campos del modal
        document.getElementById('espacioDeportivoNombre').value = espacioNombre;
        document.getElementById('espacioDeportivoId').value = espacioId;
        document.getElementById('horarioEntrada').value = formatoDatetimeLocal(inicio);
        document.getElementById('horarioSalida').value = formatoDatetimeLocal(fin);

        // Cargar coordinadores disponibles
        cargarCoordinadoresDisponibles(formatoDatetimeLocal(inicio), formatoDatetimeLocal(fin));

        // Mostrar modal
        const modal = new bootstrap.Modal(document.getElementById('modalCrearAsistencia'));
        modal.show();
    };

    // Función para cargar coordinadores disponibles
    window.cargarCoordinadoresDisponibles = function(horarioEntrada, horarioSalida) {
        const select = document.getElementById('coordinadorId');
        const alert = document.getElementById('alertCoordinadoresDisponibles');
        const textoAlert = document.getElementById('textoCoordinadoresDisponibles');

        // Limpiar select
        select.innerHTML = '<option value="">Cargando coordinadores...</option>';
        alert.classList.add('d-none');

        // Realizar petición AJAX
        fetch(`/admin/coordinadores-disponibles?horarioEntrada=${encodeURIComponent(horarioEntrada)}&horarioSalida=${encodeURIComponent(horarioSalida)}`)
            .then(response => response.json())
            .then(coordinadores => {
                select.innerHTML = '<option value="">Seleccionar coordinador...</option>';
                
                if (coordinadores.length === 0) {
                    select.innerHTML += '<option value="" disabled>No hay coordinadores disponibles</option>';
                    textoAlert.textContent = 'No hay coordinadores disponibles para este horario. Todos los coordinadores tienen asistencias asignadas que se superponen con este rango de tiempo.';
                    alert.classList.remove('d-none');
                } else {
                    coordinadores.forEach(coordinador => {
                        const option = document.createElement('option');
                        option.value = coordinador.usuarioId;
                        option.textContent = `${coordinador.nombres} ${coordinador.apellidos}`;
                        select.appendChild(option);
                    });
                    
                    textoAlert.textContent = `${coordinadores.length} coordinador(es) disponible(s) para este horario.`;
                    alert.classList.remove('d-none');
                }
            })
            .catch(error => {
                console.error('Error al cargar coordinadores:', error);
                select.innerHTML = '<option value="" disabled>Error al cargar coordinadores</option>';
                textoAlert.textContent = 'Error al cargar la lista de coordinadores disponibles.';
                alert.classList.remove('d-none');
            });
    };

    v.CalendarPage = new e,
    v.CalendarPage.Constructor = e
}(window.jQuery),

function() {
    "use strict";
    window.jQuery.CalendarPage.init();
}();
