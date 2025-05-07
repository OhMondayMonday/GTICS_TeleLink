// Donut Chart
var donutEl = document.querySelector("#donut-chart");
var puntualidad = donutEl ? parseInt(donutEl.dataset.puntualidad) || 89 : 89;
var tardanzas = donutEl ? parseInt(donutEl.dataset.tardanzas) || 11 : 11;

var options = {
    series: [puntualidad, tardanzas],
    chart: {height:250, type:"donut"},
    labels: ["Puntual", "Tardanza"],
    plotOptions: {pie:{donut:{size:"75%"}}},
    dataLabels: {enabled:false},
    legend: {show:false},
    colors: ["#34c38f","#f46a6a"]
};

var chart = new ApexCharts(donutEl, options);
chart.render();

// Calendario con carga dinámica, formulario y formateo en frontend
!function($) {
    "use strict";

    function CalendarPage() {}

    CalendarPage.prototype.init = function() {
        var calendarEl = document.getElementById("calendar");

        // Formateador de hora
        function formatTime(dateTime) {
            return new Date(dateTime).toLocaleTimeString('es-PE', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        }

        // Clase CSS según estado
        function getEventClass(estado) {
            switch(estado) {
                case 'puntual': return 'bg-success';
                case 'tarde': return 'bg-warning';
                case 'pendiente': return 'bg-info';
                case 'inasistencia': return 'bg-danger';
                default: return 'bg-primary';
            }
        }

        // Formateador del título del evento
        function formatEventTitle(asistencia) {
            const espacio = asistencia.espacioDeportivo;
            const establecimiento = espacio.establecimientoDeportivo;
            const servicio = espacio.servicioDeportivo;

            return [
                `${formatTime(asistencia.horarioEntrada)} - ${formatTime(asistencia.horarioSalida)}`,
                `Establecimiento: ${establecimiento.establecimientoDeportivoNombre}`,
                `Servicio: ${servicio.servicioDeportivo}`,
                `Espacio: ${espacio.nombre}`
            ].join('\n');
        }

        // Configuración del calendario
        var calendar = new FullCalendar.Calendar(calendarEl, {
            plugins: ["bootstrap", "interaction", "dayGrid", "timeGrid"],
            editable: false,
            droppable: false,
            selectable: false,
            initialView: "timeGridWeek",
            themeSystem: "bootstrap",
            allDaySlot: false,
            locale: 'es',
            slotMinTime: "06:00:00",
            slotMaxTime: "22:00:00",
            headerToolbar: {
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
            events: function(fetchInfo, successCallback, failureCallback) {
                const userId = obtenerUserIdDeSesion();

                console.log('Buscando asistencias entre:', fetchInfo.start, 'y', fetchInfo.end);

                $.ajax({
                    url: '/coordinador/calendarioCoordi',
                    method: 'GET',
                    data: {
                        start: fetchInfo.start.toISOString(),
                        end: fetchInfo.end.toISOString(),
                        userId: userId
                    },
                    success: function(asistencias) {
                        console.log('Asistencias recibidas:', asistencias);

                        const eventos = asistencias.map(asistencia => {
                            try {
                                return {
                                    title: formatEventTitle(asistencia),
                                    start: asistencia.horarioEntrada,
                                    end: asistencia.horarioSalida,
                                    className: getEventClass(asistencia.estadoEntrada.toString()),
                                    extendedProps: {
                                        coordinador: `${asistencia.coordinador.nombres} ${asistencia.coordinador.apellidos}`,
                                        espacio: asistencia.espacioDeportivo.nombre,
                                        estado: asistencia.estadoEntrada.toString()
                                    }
                                };
                            } catch (error) {
                                console.error('Error procesando asistencia:', asistencia, error);
                                return null;
                            }
                        }).filter(evento => evento !== null);

                        console.log('Eventos generados:', eventos);
                        successCallback(eventos);
                    },
                    error: function(xhr, status, error) {
                        console.error('Error al obtener asistencias:', error);
                        failureCallback(error);
                    }
                });
            },
            eventDidMount: function(info) {
                $(info.el).tooltip({
                    title: info.event.title.replace(/\n/g, '<br>'),
                    placement: 'top',
                    trigger: 'hover',
                    html: true,
                    container: 'body'
                });
            },
            eventClick: function(info) {
                info.jsEvent.preventDefault();

                const event = info.event;
                const content = `
                    <div class="p-3">
                        <h5>${event.extendedProps.espacio}</h5>
                        <hr>
                        <p><strong>Horario:</strong> ${event.start.toLocaleTimeString('es-PE')} - ${event.end.toLocaleTimeString('es-PE')}</p>
                        <p><strong>Coordinador:</strong> ${event.extendedProps.coordinador}</p>
                        <p><strong>Estado:</strong> ${formatEstado(event.extendedProps.estado)}</p>
                    </div>
                `;

                Swal.fire({
                    title: 'Detalles de Asistencia',
                    html: content,
                    confirmButtonText: 'Cerrar',
                    customClass: {
                        popup: 'text-left'
                    }
                });
            }
        });

        calendar.render();

        // Función para formatear el estado
        function formatEstado(estado) {
            const estados = {
                'puntual': 'Puntual',
                'tarde': 'Tardanza',
                'pendiente': 'Pendiente',
                'inasistencia': 'Inasistencia'
            };
            return estados[estado] || estado;
        }

        // Función para obtener userId
        function obtenerUserIdDeSesion() {
            return 11; // Valor temporal para pruebas
        }

        // Lógica del formulario de nueva asistencia
        const fechaInput = document.getElementById('fecha');
        const establecimientoSelect = document.getElementById('establecimiento');
        const servicioSelect = document.getElementById('servicio');
        const espacioSelect = document.getElementById('espacio');
        const horaEntradaSelect = document.getElementById('horaEntrada');
        const horaSalidaSelect = document.getElementById('horaSalida');
        const observacionInput = document.getElementById('observacion');
        const asistenciaForm = document.getElementById('asistenciaForm');

        // Establecer fecha mínima (día siguiente)
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        fechaInput.min = tomorrow.toISOString().split('T')[0];

        // Función para generar opciones de hora cada 30 minutos
        function generarOpcionesHoras(apertura, cierre) {
            horaEntradaSelect.innerHTML = '<option value="">Seleccione una hora</option>';
            horaSalidaSelect.innerHTML = '<option value="">Seleccione una hora</option>';

            const aperturaParts = apertura.split(':');
            const cierreParts = cierre.split(':');
            const aperturaHoras = parseInt(aperturaParts[0]);
            const aperturaMinutos = parseInt(aperturaParts[1]);
            const cierreHoras = parseInt(cierreParts[0]);
            const cierreMinutos = parseInt(cierreParts[1]);

            let hora = aperturaHoras;
            let minuto = aperturaMinutos;

            while (hora < cierreHoras || (hora === cierreHoras && minuto <= cierreMinutos)) {
                const timeString = `${hora.toString().padStart(2, '0')}:${minuto.toString().padStart(2, '0')}`;
                const optionEntrada = document.createElement('option');
                const optionSalida = document.createElement('option');
                optionEntrada.value = timeString;
                optionSalida.value = timeString;
                optionEntrada.text = timeString;
                optionSalida.text = timeString;
                horaEntradaSelect.appendChild(optionEntrada);
                horaSalidaSelect.appendChild(optionSalida);

                minuto += 30;
                if (minuto >= 60) {
                    hora += 1;
                    minuto = 0;
                }
            }
        }

        // Cargar establecimientos
        $.ajax({
            url: '/coordinador/establecimientos',
            method: 'GET',
            success: function(establecimientos) {
                establecimientos.forEach(est => {
                    const option = document.createElement('option');
                    option.value = est.establecimientoDeportivoId;
                    option.text = est.establecimientoDeportivoNombre;
                    option.dataset.horarioApertura = est.horarioApertura;
                    option.dataset.horarioCierre = est.horarioCierre;
                    establecimientoSelect.appendChild(option);
                });
            },
            error: function(xhr, status, error) {
                console.error('Error al cargar establecimientos:', error);
                Swal.fire('Error', 'No se pudieron cargar los establecimientos', 'error');
            }
        });

        // Cargar servicios al seleccionar un establecimiento
        establecimientoSelect.addEventListener('change', function() {
            const establecimientoId = this.value;
            servicioSelect.disabled = true;
            espacioSelect.disabled = true;
            servicioSelect.innerHTML = '<option value="">Seleccione un servicio</option>';
            espacioSelect.innerHTML = '<option value="">Seleccione un espacio</option>';
            horaEntradaSelect.disabled = true;
            horaSalidaSelect.disabled = true;

            if (establecimientoId) {
                $.ajax({
                    url: '/coordinador/servicios-por-establecimiento',
                    method: 'GET',
                    data: { establecimientoId: establecimientoId },
                    success: function(servicios) {
                        servicios.forEach(serv => {
                            const option = document.createElement('option');
                            option.value = serv.servicioDeportivoId;
                            option.text = serv.servicioDeportivo;
                            servicioSelect.appendChild(option);
                        });
                        servicioSelect.disabled = false;
                    },
                    error: function(xhr, status, error) {
                        console.error('Error al cargar servicios:', error);
                        Swal.fire('Error', 'No se pudieron cargar los servicios', 'error');
                    }
                });

                // Generar opciones de hora según el establecimiento
                const selectedOption = this.options[this.selectedIndex];
                generarOpcionesHoras(selectedOption.dataset.horarioApertura, selectedOption.dataset.horarioCierre);
                horaEntradaSelect.disabled = false;
                horaSalidaSelect.disabled = false;
            }
        });

        // Cargar espacios al seleccionar un servicio
        servicioSelect.addEventListener('change', function() {
            const servicioId = this.value;
            const establecimientoId = establecimientoSelect.value;
            espacioSelect.disabled = true;
            espacioSelect.innerHTML = '<option value="">Seleccione un espacio</option>';

            if (servicioId && establecimientoId) {
                $.ajax({
                    url: '/coordinador/espacios-por-servicio',
                    method: 'GET',
                    data: {
                        establecimientoId: establecimientoId,
                        servicioId: servicioId
                    },
                    success: function(espacios) {
                        espacios.forEach(esp => {
                            const option = document.createElement('option');
                            option.value = esp.espacioDeportivoId;
                            option.text = esp.nombre;
                            espacioSelect.appendChild(option);
                        });
                        espacioSelect.disabled = false;
                    },
                    error: function(xhr, status, error) {
                        console.error('Error al cargar espacios:', error);
                        Swal.fire('Error', 'No se pudieron cargar los espacios', 'error');
                    }
                });
            }
        });

        // Manejar el envío del formulario
        asistenciaForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const fecha = fechaInput.value;
            const espacioId = espacioSelect.value;
            const horaEntrada = horaEntradaSelect.value;
            const horaSalida = horaSalidaSelect.value;
            const observacion = observacionInput.value;

            // Combinar fecha y hora para crear LocalDateTime
            const horarioEntrada = `${fecha}T${horaEntrada}:00`;
            const horarioSalida = `${fecha}T${horaSalida}:00`;

            // Validar que la hora de entrada sea menor que la de salida (frontend)
            if (new Date(horarioEntrada) >= new Date(horarioSalida)) {
                Swal.fire('Error', 'La hora de entrada debe ser menor que la hora de salida', 'error');
                return;
            }

            // Enviar datos al servidor
            $.ajax({
                url: '/coordinador/asistencia',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    espacioDeportivoId: parseInt(espacioId),
                    horarioEntrada: horarioEntrada,
                    horarioSalida: horarioSalida,
                    observacionAsistencia: observacion
                }),
                success: function(response) {
                    Swal.fire('Éxito', 'Asistencia registrada correctamente', 'success');
                    $('#asistenciaModal').modal('hide');
                    asistenciaForm.reset();
                    calendar.refetchEvents(); // Actualizar calendario
                },
                error: function(xhr, status, error) {
                    console.error('Error al registrar asistencia:', error, xhr.responseText);
                    let errorMessage = 'No se pudo registrar la asistencia';
                    if (xhr.responseJSON && xhr.responseJSON.message) {
                        errorMessage = xhr.responseJSON.message;
                    }
                    Swal.fire('Error', errorMessage, 'error');
                }
            });
        });

        // Actualizar calendario cada 5 minutos
        setInterval(function() {
            console.log('Actualizando calendario...');
            calendar.refetchEvents();
        }, 300000);
    };

    $.CalendarPage = new CalendarPage();
    $.CalendarPage.Constructor = CalendarPage;
}(window.jQuery);

$(function() {
    "use strict";
    $.CalendarPage.init();
});