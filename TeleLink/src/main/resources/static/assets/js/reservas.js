document.addEventListener('DOMContentLoaded', function() {
    // Obtener el servicio desde la URL
    const urlParams = new URLSearchParams(window.location.search);
    const servicioIdParam = urlParams.get('servicio');

    // Definir todos los servicios disponibles en el sistema
    const mapeoServicios = {
        'piscina': { id: 'piscina', nombre: 'Piscina', icon: '🏊' },
        'gimnasio': { id: 'gimnasio', nombre: 'Gimnasio', icon: '🏋️' },
        'futbol-loza': { id: 'futbol-loza', nombre: 'Fútbol Loza', icon: '⚽' },
        'futbol-grass': { id: 'futbol-grass', nombre: 'Fútbol Grass', icon: '⚽' },
        'atletismo': { id: 'atletismo', nombre: 'Atletismo', icon: '🏃' },
        'multiple': { id: 'multiple', nombre: 'Cancha Multiusos', icon: '🎯' }
    };

    // Definir servicios disponibles para mostrar en los selectores
    const servicios = [
        mapeoServicios['piscina'],
        mapeoServicios['gimnasio'],
        mapeoServicios['futbol-loza'],
        mapeoServicios['futbol-grass'],
        mapeoServicios['atletismo'],
        mapeoServicios['multiple']
    ];

    // Establecer el servicio seleccionado según el parámetro, o usar el predeterminado
    let servicioSeleccionado = servicioIdParam && mapeoServicios[servicioIdParam]
        ? mapeoServicios[servicioIdParam]
        : servicios[0];

    // Ajustar el layout si venimos con un servicio específico
    if (servicioIdParam) {
        // Ocultar los selectores de servicio
        document.querySelector('.desktop-services').style.display = 'none';
        document.querySelector('.mobile-services').style.display = 'none';

        // Ajustar el ancho del contenedor del calendario
        document.getElementById('calendar-column').className = 'col-12';

        // Actualizar el título para mostrar el servicio
        document.querySelector('h2').textContent = `Reserva de ${servicioSeleccionado.nombre}`;
    }

    // Variables de estado
    let fechaActual = new Date();
    let reservas = {};
    let horasSeleccionadas = [];
    let cargando = true;

    // Generar las horas del día (6am a 22pm)
    const horas = Array.from({ length: 17 }, (_, i) => i + 6);

    // Inicializar componentes
    inicializarServicios();
    actualizarCalendario();

    // Event listeners para navegación de fecha
    document.getElementById('prev-day').addEventListener('click', irAlDiaAnterior);
    document.getElementById('next-day').addEventListener('click', irAlDiaSiguiente);

    // Event listener para el botón de continuar
    document.getElementById('continue-btn').addEventListener('click', continuarReserva);

    // Función para inicializar los servicios
    function inicializarServicios() {
        // Si tenemos un servicio específico, no necesitamos inicializar los selectores
        if (servicioIdParam) {
            return;
        }

        // Llenar la lista de servicios (desktop)
        const serviceList = document.getElementById('service-list');
        serviceList.innerHTML = '';

        servicios.forEach(servicio => {
            const serviceItem = document.createElement('div');
            serviceItem.className = `service-item ${servicio.id === servicioSeleccionado.id ? 'active' : ''}`;
            serviceItem.dataset.id = servicio.id;

            serviceItem.innerHTML = `
                <span class="service-icon">${servicio.icon}</span>
                <span>${servicio.nombre}</span>
                ${servicio.id === servicioSeleccionado.id ? '<span class="ms-auto text-success">✓</span>' : ''}
            `;

            serviceItem.addEventListener('click', () => {
                cambiarServicio(servicio);
            });

            serviceList.appendChild(serviceItem);
        });

        // Llenar la lista de servicios móvil
        const mobileServices = document.querySelector('.mobile-services');
        mobileServices.innerHTML = '';

        servicios.forEach(servicio => {
            const mobileServiceBtn = document.createElement('button');
            mobileServiceBtn.className = `mobile-service-btn ${servicio.id === servicioSeleccionado.id ? 'active' : ''}`;
            mobileServiceBtn.dataset.id = servicio.id;

            mobileServiceBtn.innerHTML = `
                <span class="mobile-service-icon">${servicio.icon}</span>
                <span class="mobile-service-name">${servicio.nombre}</span>
            `;

            mobileServiceBtn.addEventListener('click', () => {
                cambiarServicio(servicio);
            });

            mobileServices.appendChild(mobileServiceBtn);
        });
    }

    // Función para cambiar el servicio seleccionado
    function cambiarServicio(servicio) {
        servicioSeleccionado = servicio;
        horasSeleccionadas = [];
        inicializarServicios(); // Actualizar la UI de selección de servicio
        actualizarCalendario();
    }

    // Función para ir al día anterior
    function irAlDiaAnterior() {
        fechaActual.setDate(fechaActual.getDate() - 1);
        actualizarCalendario();
    }

    // Función para ir al día siguiente
    function irAlDiaSiguiente() {
        fechaActual.setDate(fechaActual.getDate() + 1);
        actualizarCalendario();
    }

    // Función para formatear la fecha en formato ISO para enviar al servidor
    function formatearFechaISO(fecha) {
        return fecha.toISOString().split('T')[0]; // YYYY-MM-DD
    }

    // Función para formatear la fecha en formato de visualización
    function formatearFecha(fecha) {
        const dia = fecha.getDate().toString().padStart(2, '0');
        const mes = (fecha.getMonth() + 1).toString().padStart(2, '0');
        return `${dia}/${mes}`;
    }

    // Función para obtener el nombre del día
    function obtenerNombreDia(fecha) {
        const dias = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
        return dias[fecha.getDay()];
    }

    // Función para actualizar el calendario
    function actualizarCalendario() {
        // Actualizar fecha mostrada
        document.getElementById('current-date').textContent = `${obtenerNombreDia(fechaActual)} ${formatearFecha(fechaActual)}`;

        // Mostrar cargando
        cargando = true;
        const calendar = document.getElementById('calendar');
        calendar.innerHTML = `
            <div style="height: 100px; position: relative;">
                <div class="loading-overlay">
                    <div class="spinner"></div>
                    <p class="mt-2">Cargando...</p>
                </div>
            </div>
        `;

        // Realizar la petición AJAX para obtener los datos de disponibilidad
        cargarDisponibilidad();
    }

    // Función para cargar la disponibilidad del servidor
    function cargarDisponibilidad() {
        const fechaISO = formatearFechaISO(fechaActual);

        // Realizar la petición AJAX
        fetch(`/api/reservas/disponibilidad?servicioId=${servicioSeleccionado.id}&fecha=${fechaISO}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Error al obtener disponibilidad');
                }
                return response.json();
            })
            .then(data => {
                reservas = {
                    [servicioSeleccionado.id]: data
                };
                cargando = false;
                renderizarCalendario();
            })
            .catch(error => {
                console.error('Error:', error);
                cargando = false;

                // En caso de error, usamos datos simulados (solo para desarrollo)
                generarDatosDeReserva();
                renderizarCalendario();

                // Mostrar mensaje de error
                const errorDiv = document.createElement('div');
                errorDiv.className = 'alert alert-danger';
                errorDiv.textContent = 'Error al cargar disponibilidad. Usando datos simulados.';
                document.getElementById('calendar').before(errorDiv);

                // Eliminar el mensaje después de 5 segundos
                setTimeout(() => errorDiv.remove(), 5000);
            });
    }

    // Función para generar datos simulados de reserva (para fallback)
    function generarDatosDeReserva() {
        reservas = {};

        // Generar reservas para cada servicio
        Object.keys(mapeoServicios).forEach(servicioId => {
            reservas[servicioId] = {};

            // Generar algunos bloques aleatorios de reservas
            const numBloques = Math.floor(Math.random() * 4) + 1;

            for (let i = 0; i < numBloques; i++) {
                const horaInicio = Math.floor(Math.random() * 13) + 8; // Entre 8 y 20
                const duracion = Math.floor(Math.random() * 3) + 1; // Entre 1 y 3 horas

                for (let h = horaInicio; h < horaInicio + duracion && h < 22; h++) {
                    reservas[servicioId][h] = {
                        disponible: false,
                        esUsuarioActual: Math.random() > 0.9 // Algunas reservas son del usuario actual
                    };
                }
            }
        });
    }

    // Función para renderizar el calendario
    function renderizarCalendario() {
        const calendar = document.getElementById('calendar');
        calendar.innerHTML = '';

        // Crear la fila de cabecera (horas)
        const headerRow = document.createElement('div');
        headerRow.className = 'hour-headers';

        // Espacio para la columna de servicio
        const serviceHeader = document.createElement('div');
        serviceHeader.className = 'service-label';
        headerRow.appendChild(serviceHeader);

        // Crear las cabeceras de horas
        horas.forEach(hora => {
            const hourHeader = document.createElement('div');
            hourHeader.className = 'hour-label';
            hourHeader.textContent = hora;
            headerRow.appendChild(hourHeader);
        });

        calendar.appendChild(headerRow);

        // Crear la fila de reservas para el servicio seleccionado
        const rowContainer = document.createElement('div');
        rowContainer.className = 'reservation-row';

        // Etiqueta del servicio
        const serviceLabel = document.createElement('div');
        serviceLabel.className = 'service-label';
        serviceLabel.innerHTML = `<span class="fw-medium">${servicioSeleccionado.nombre}</span>`;
        rowContainer.appendChild(serviceLabel);

        // Contenedor para las celdas interactivas
        const interactiveCells = document.createElement('div');
        interactiveCells.className = 'interactive-cells';

        // Crear las barras de reserva
        if (reservas[servicioSeleccionado.id]) {
            Object.entries(reservas[servicioSeleccionado.id]).forEach(([hora, datos]) => {
                const horaNum = parseInt(hora);
                if (horaNum >= 6 && horaNum <= 22) {
                    const reservationBar = document.createElement('div');
                    reservationBar.className = `reservation-bar ${datos.esUsuarioActual ? 'tu-reserva' : 'no-disponible'}`;

                    // Calcular posición y ancho
                    const leftPos = ((horaNum - 6) / 17) * 100;
                    const width = (1 / 17) * 100;

                    reservationBar.style.left = `${leftPos}%`;
                    reservationBar.style.width = `${width}%`;

                    interactiveCells.appendChild(reservationBar);
                }
            });
        }

        // Crear las celdas interactivas para cada hora
        horas.forEach(hora => {
            const hourCell = document.createElement('div');
            hourCell.className = 'hour-cell';
            hourCell.dataset.hora = hora;

            const estaReservado = reservas[servicioSeleccionado.id]?.[hora]?.disponible === false;
            const estaSeleccionado = horasSeleccionadas.includes(hora);

            if (estaReservado) {
                hourCell.classList.add('no-disponible');
            }

            if (estaSeleccionado) {
                hourCell.classList.add('seleccionado');
            }

            // Añadir evento de clic solo si no está reservado
            if (!estaReservado) {
                hourCell.addEventListener('click', () => toggleSeleccionHora(hora));
            }

            interactiveCells.appendChild(hourCell);
        });

        rowContainer.appendChild(interactiveCells);
        calendar.appendChild(rowContainer);

        // Actualizar información de horas seleccionadas
        actualizarInfoSeleccion();
    }

    // Función para alternar la selección de una hora
    function toggleSeleccionHora(hora) {
        const indice = horasSeleccionadas.indexOf(hora);

        if (indice === -1) {
            // Añadir hora a la selección
            horasSeleccionadas.push(hora);
        } else {
            // Quitar hora de la selección
            horasSeleccionadas.splice(indice, 1);
        }

        // Actualizar la visualización
        renderizarCalendario();
    }

    // Función para actualizar la información de selección
    function actualizarInfoSeleccion() {
        const infoElement = document.getElementById('selected-hours-info');
        const continueBtn = document.getElementById('continue-btn');

        if (horasSeleccionadas.length === 0) {
            infoElement.innerHTML = '';
            continueBtn.disabled = true;
        } else {
            // Ordenar las horas y formatearlas
            const horasOrdenadas = [...horasSeleccionadas].sort((a, b) => a - b);
            const horasFormateadas = horasOrdenadas.map(h => `${h}:00`).join(', ');

            infoElement.innerHTML = `<strong>Horas seleccionadas:</strong> ${horasFormateadas}`;
            continueBtn.disabled = false;
        }
    }

    // Función para continuar con la reserva
    function continuarReserva() {
        if (horasSeleccionadas.length === 0) {
            alert('Por favor selecciona al menos una hora');
            return;
        }

        // Preparar datos para enviar al servidor
        const datosReserva = {
            servicioId: servicioSeleccionado.id,
            servicioNombre: servicioSeleccionado.nombre,
            fecha: formatearFechaISO(fechaActual),
            horas: horasSeleccionadas
        };

        // Mostrar indicador de cargando
        const continueBtn = document.getElementById('continue-btn');
        const btnTextoOriginal = continueBtn.textContent;
        continueBtn.disabled = true;
        continueBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Procesando...';

        // Enviar la solicitud AJAX para crear la reserva
        fetch('/api/reservas', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
            },
            body: JSON.stringify(datosReserva)
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.error || 'Error al crear la reserva');
                    });
                }
                return response.json();
            })
            .then(data => {
                // Guardar en localStorage como respaldo
                localStorage.setItem('reserva_confirmada', JSON.stringify({
                    ...datosReserva,
                    reservaIds: data.reservas.map(r => r.reservaId),
                    timestamp: new Date().getTime()
                }));

                // Mostrar mensaje de éxito
                const alertDiv = document.createElement('div');
                alertDiv.className = 'alert alert-success';
                alertDiv.innerHTML = `<strong>¡Reserva exitosa!</strong> Tu reserva ha sido confirmada.`;
                document.querySelector('.calendar-container').before(alertDiv);

                // Redireccionar a la página de mis reservas después de 2 segundos
                setTimeout(() => {
                    window.location.href = 'vecino-mis-reservas.html';
                }, 2000);
            })
            .catch(error => {
                console.error('Error:', error);

                // Mostrar mensaje de error
                const alertDiv = document.createElement('div');
                alertDiv.className = 'alert alert-danger';
                alertDiv.textContent = error.message || 'Error al procesar la reserva. Intente nuevamente.';
                document.querySelector('.calendar-container').before(alertDiv);

                // Restaurar el botón
                continueBtn.innerHTML = btnTextoOriginal;
                continueBtn.disabled = false;

                // Eliminar el mensaje después de 5 segundos
                setTimeout(() => alertDiv.remove(), 5000);
            });
    }
    fetch('/api/reservas', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        },
        body: JSON.stringify(datosReserva)
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(data => {
                    throw new Error(data.error || 'Error al crear la reserva');
                });
            }
            return response.json();
        })
        .then(data => {
            // Confirmación de reserva exitosa
        })
        .catch(error => {
            console.error('Error:', error);
            // Mostrar error
        });

});