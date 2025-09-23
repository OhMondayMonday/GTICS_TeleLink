/**
 * Configuración moderna para FullCalendar v5
 * Este archivo contiene la configuración recomendada para el calendario de reservas
 */

// Configuración base para todas las instancias de FullCalendar
const calendarDefaultConfig = {
    // Licencia para FullCalendar Scheduler
    schedulerLicenseKey: 'CC-Attribution-NonCommercial-NoDerivatives',
    
    // Vista inicial
    initialView: 'timeGridWeek',
    
    // Configuración regional
    locale: 'es',
    
    // Barra de herramientas - solo vista semanal
    headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: '' // Eliminamos las opciones de vista
    },
    
    // Textos de botones
    buttonText: {
        today: 'Hoy'
    },
    
    // Las horas se configurarán dinámicamente en la función initializeCalendar
    // según el espacio deportivo seleccionado
    
    allDaySlot: false,
    height: '100%',
    slotDuration: '01:00:00',
    slotLabelInterval: '01:00',
    
    // Formato de etiquetas de hora (24h)
    slotLabelFormat: {
        hour: '2-digit',
        minute: '2-digit',
        omitZeroMinute: false,
        hour12: false
    },
    
    // Opciones de interactividad
    editable: false,
    droppable: false,
    selectable: true,
    eventStartEditable: false,
    eventDurationEditable: false,    
    // Mostrar indicador de hora actual
    nowIndicator: true,
    
    // Restricción de navegación - no permitir ver semanas pasadas
    validRange: function() {
        const now = new Date();
        const startOfWeek = new Date(now);
        startOfWeek.setDate(now.getDate() - now.getDay() + 1); // Lunes de esta semana
        startOfWeek.setHours(0, 0, 0, 0);
        
        return {
            start: startOfWeek.toISOString().split('T')[0]
        };
    }
};

/**
 * Función para inicializar un calendario con configuración personalizada
 * @param {HTMLElement} calendarEl - Elemento DOM donde se renderizará el calendario
 * @param {Object} customConfig - Configuración personalizada para fusionar con la configuración predeterminada
 * @param {string} openingTime - Hora de apertura del espacio (formato HH:MM:SS)
 * @param {string} closingTime - Hora de cierre del espacio (formato HH:MM:SS)
 * @returns {Object} Instancia del calendario
 */
function initializeCalendar(calendarEl, customConfig = {}, openingTime = '07:00:00', closingTime = '22:00:00') {
    // Combinar configuración base con la personalizada
    const config = { 
        ...calendarDefaultConfig,
        ...customConfig,
        // Sobrescribir horas de inicio y fin con los valores del espacio deportivo
        slotMinTime: openingTime,
        slotMaxTime: closingTime
    };
    
    // Crear y devolver la instancia del calendario
    const calendar = new FullCalendar.Calendar(calendarEl, config);
    return calendar;
}

/**
 * Función para marcar horarios pasados como no disponibles
 * @param {Object} calendar - Instancia del calendario
 */
function marcarHorariosPasadosComoNoDisponibles(calendar) {
    // Obtener fecha actual para determinar el inicio de la semana
    const now = new Date();
    
    // Calcular el inicio de la semana (lunes)
    const inicioSemana = new Date(now);
    const diaSemana = inicioSemana.getDay(); // 0 = domingo, 1 = lunes, ...
    const diasHastaLunes = diaSemana === 0 ? 6 : diaSemana - 1;
    
    inicioSemana.setDate(inicioSemana.getDate() - diasHastaLunes);
    inicioSemana.setHours(2, 0, 0, 0); // 7:00 AM
    
    // Si el inicio es posterior a la hora actual, no hay nada que marcar
    if (inicioSemana >= now) return;
    
    // Eliminar eventos existentes con ID específico
    const eventoExistente = calendar.getEventById('horario-pasado');
    if (eventoExistente) {
        eventoExistente.remove();
    }
    
    // Crear un evento de fondo para marcar el período pasado
    calendar.addEvent({
        id: 'horario-pasado',
        title: 'No disponible',
        start: inicioSemana,
        end: now,
        display: 'background',
        backgroundColor: 'rgba(203, 213, 225, 0.5)',
        borderColor: 'rgba(148, 163, 184, 0.7)',
        textColor: '#64748b',
        className: 'evento-fantasma',
        editable: false,
        extendedProps: {
            esFantasma: true,
            esPasada: true
        }
    });
}
