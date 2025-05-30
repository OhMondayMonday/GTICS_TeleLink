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
                    console.log('Asistencias:', asistencias);

                    const eventos = [
                        ...reservas.map(reserva => ({
                            title: `Reservado (${reserva.horario})`,
                            start: reserva.inicio,
                            end: reserva.fin,
                            backgroundColor: '#556ee6', // primary
                            borderColor: '#556ee6',
                            extendedProps: {
                                tipo: 'reserva',
                                horario: reserva.horario
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
            },
            eventClick: function(info) {
                info.jsEvent.preventDefault();

                const event = info.event;
                const tipo = event.extendedProps.tipo;

                let content = `<div class="p-3">`;
                if (tipo === 'reserva') {
                    content += `
                        <h5>Reserva</h5>
                        <hr>
                        <p><strong>Horario:</strong> ${event.extendedProps.horario}</p>
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
        });

        calendar.render();

        // Actualizar cada 5 minutos
        setInterval(function() {
            console.log('Actualizando calendario...');
            calendar.refetchEvents();
        }, 300000);
    },

    v.CalendarPage = new e,
    v.CalendarPage.Constructor = e
}(window.jQuery),

function() {
    "use strict";
    window.jQuery.CalendarPage.init();
}();
