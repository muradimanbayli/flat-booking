package net.imanbayli.flat.booking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.imanbayli.flat.booking.model.ErrorResponse;
import net.imanbayli.flat.booking.model.Flat;
import net.imanbayli.flat.booking.model.Landlord;
import net.imanbayli.flat.booking.model.ReserveSlot;
import net.imanbayli.flat.booking.repository.FlatRepository;
import net.imanbayli.flat.booking.repository.provider.FlatRepositoryInMemoryProvider;
import net.imanbayli.flat.booking.service.FlatService;
import net.imanbayli.flat.booking.service.NotificationService;
import net.imanbayli.flat.booking.service.provider.FlatServiceDefaultProvider;
import net.imanbayli.flat.booking.service.provider.NotificationServiceStubProvider;
import spark.Request;
import spark.Response;
import spark.Spark;

public class Application {
    static FlatRepository flatRepository = new FlatRepositoryInMemoryProvider();
    static NotificationService notificationService = new NotificationServiceStubProvider();
    static FlatService flatService = new FlatServiceDefaultProvider(flatRepository, notificationService);
    static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        loadDummyData();
        startServer();
    }

    public static void startServer(){
        Spark.port(8080);
        Spark.post("/v1/flat/:flatId/reserve", (request, response) -> {
            response.header("Content-Type", "application/json");
            String flatId = request.params(":flatId");
            ReserveSlot slot = mapper.readValue(request.body(), ReserveSlot.class);
            return mapper.writeValueAsString(flatService.reserve(flatId, slot));
        });
        Spark.patch("/v1/flat/:flatId/cancel/:reservationId", (request, response) -> {
            response.header("Content-Type", "application/json");
            String flatId = request.params(":flatId");
            String reservationId = request.params(":reservationId");
            return mapper.writeValueAsString(flatService.cancel(flatId, reservationId));
        });
        Spark.patch("/v1/flat/:flatId/approve/:reservationId", (request, response) -> {
            response.header("Content-Type", "application/json");
            String flatId = request.params(":flatId");
            String reservationId = request.params(":reservationId");
            return mapper.writeValueAsString(flatService.approve(flatId, reservationId));
        });
        Spark.patch("/v1/flat/:flatId/reject/:reservationId", (request, response) -> {
            response.header("Content-Type", "application/json");
            String flatId = request.params(":flatId");
            String reservationId = request.params(":reservationId");
            return mapper.writeValueAsString(flatService.reject(flatId, reservationId));
        });
        Spark.get("/v1/flat/:flatId/view", (request, response) -> {
            response.header("Content-Type", "application/json");
            String flatId = request.params(":flatId");
            return mapper.writeValueAsString(flatService.viewOccupiedDates(flatId));
        });

        Spark.exception(Exception.class, Application::handle);
    }

    private static void handle(Exception exception, Request request, Response response) {
        //if(Set.of(FlatNotFoundException.class, IllegalTimeslotException.class, ReservationNotFoundException.class))
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(exception.getMessage());
        response.header("Content-Type", "application/json");
        response.status(400);
        try {
            response.body(mapper.writeValueAsString(errorResponse));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private static void loadDummyData() {
        Landlord landlord = new Landlord("ID_LANDLORD_1", "Murad", "Imanbayli");
        Flat flat = new Flat("ID_FLAT_1", "Cozy apartment", "London");
        flat.setLandlord(landlord);
        flatRepository.save(flat);
        System.out.println("Flat loaded: " + flat);

        flat = new Flat("ID_FLAT_2", "Super cheap flat in center", "London");
        flat.setLandlord(landlord);
        flatRepository.save(flat);
        System.out.println("Flat loaded: " + flat);
    }
}