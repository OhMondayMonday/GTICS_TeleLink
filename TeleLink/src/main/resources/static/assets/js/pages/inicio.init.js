// Donut Chart
var donutEl = document.querySelector("#donut-chart");
var legendEl = document.querySelector("#donut-chart-legend");

function renderDonutChart(puntualidad, tardanzas, inasistencias) {
    var total = puntualidad + tardanzas + inasistencias;
    var puntualidadPct = total > 0 ? Math.round((puntualidad / total) * 100) : 0;
    var tardanzasPct = total > 0 ? Math.round((tardanzas / total) * 100) : 0;
    var inasistenciasPct = total > 0 ? Math.round((inasistencias / total) * 100) : 0;

    var options = {
        series: [puntualidad, tardanzas, inasistencias],
        chart: { height: 250, type: "donut" },
        labels: ["Puntual", "Tardanza", "Inasistencia"],
        plotOptions: { pie: { donut: { size: "75%" } } },
        dataLabels: { enabled: false },
        legend: { show: false },
        colors: ["#34c38f", "#f1b44c", "#f46a6a"] // success, warning, danger
    };

    var chart = new ApexCharts(donutEl, options);
    chart.render();

    // Generar leyenda dinámica
    legendEl.innerHTML = `
        <div class="col-4">
            <div class="text-center">
                <p class="mb-2 text-truncate"><i class="mdi mdi-circle text-success font-size-10 me-1"></i> Puntual</p>
                <h5>${puntualidadPct} %</h5>
            </div>
        </div>
        <div class="col-4">
            <div class="text-center">
                <p class="mb-2 text-truncate"><i class="mdi mdi-circle text-warning font-size-10 me-1"></i> Tardanza</p>
                <h5>${tardanzasPct} %</h5>
            </div>
        </div>
        <div class="col-4">
            <div class="text-center">
                <p class="mb-2 text-truncate"><i class="mdi mdi-circle text-danger font-size-10 me-1"></i> Inasistencia</p>
                <h5>${inasistenciasPct} %</h5>
            </div>
        </div>
    `;
}

// Cargar datos del donut chart vía AJAX
function loadDonutChartData(userId) {
    $.ajax({
        url: '/coordinador/estadisticas-asistencias',
        method: 'GET',
        data: { userId: userId },
        success: function(data) {
            console.log('Estadísticas recibidas:', data);
            var puntualidad = data.puntual || 0;
            var tardanzas = data.tarde || 0;
            var inasistencias = data.inasistencia || 0;
            renderDonutChart(puntualidad, tardanzas, inasistencias);
        },
        error: function(xhr, status, error) {
            console.error('Error al obtener estadísticas:', error);
            renderDonutChart(0, 0, 0);
        }
    });
}

// Calendario con carga dinámica y formateo en frontend
!function(v){
    "use strict";

    function e(){}

    e.prototype.init=function(){

        // Cargar donut chart
        var userId = obtenerUserIdDeSesion();
        loadDonutChartData(userId);

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
                `${establecimiento.establecimientoDeportivoNombre}: `,
                `${servicio.servicioDeportivo}`
            ].join('\n');
        }

        // Configuración del calendario
        var calendar = new FullCalendar.Calendar(calendarEl, {
            plugins: ["bootstrap", "interaction", "dayGrid", "timeGrid"],
            editable: false,
            droppable: false,
            selectable: false,
            defaultView: "timeGridWeek",
            themeSystem: "bootstrap",
            allDaySlot: false,
            locale: 'es',
            slotMinTime: "06:00:00",
            slotMaxTime: "22:00:00",
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
            events: function(fetchInfo, successCallback, failureCallback) {
                // Obtener el userId del usuario logueado (debes implementar esto)
                const userId = obtenerUserIdDeSesion();

                console.log('Buscando asistencias entre:', fetchInfo.start, 'y', fetchInfo.end);

                $.ajax({
                    url: '/coordinador/calendario',
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
            eventRender: function(info) {
                // Tooltip con mejor formato
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

        function obtenerUserIdDeSesion() {
            const userIdEl = document.getElementById('currentUserId');
            return userIdEl ? parseInt(userIdEl.value) : 6; // Fallback a 6
        }

        // Actualizar cada 5 minutos
        setInterval(function() {
            console.log('Actualizando calendario...');
            calendar.refetchEvents();
        }, 300000);
    },

        v.CalendarPage=new e,
        v.CalendarPage.Constructor=e
}(window.jQuery),

    function(){
        "use strict";
        window.jQuery.CalendarPage.init();
    }();