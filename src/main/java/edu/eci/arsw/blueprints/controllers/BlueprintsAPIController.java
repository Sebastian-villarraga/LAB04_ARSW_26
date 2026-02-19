package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(
        name = "Blueprints API",
        description = "REST operations for managing architectural blueprints "
)
@RestController
@RequestMapping("/api/v1/blueprints")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) {
        this.services = services;
    }

    @Operation(
            summary = "Retrieve all blueprints",
            description = "Returns the complete collection of blueprints stored in the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Blueprints retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<Set<Blueprint>>> getAll() {
        return ResponseEntity.ok(
                new ApiResponseWrapper<>(200, "execute ok",
                        services.getAllBlueprints())
        );
    }

    @Operation(
            summary = "Retrieve blueprints by author",
            description = "Returns all blueprints associated with a specific author."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Blueprints retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponseWrapper<Set<Blueprint>>> byAuthor(
            @Parameter(
                    description = "Author of the blueprint",
                    example = "john"
            )
            @PathVariable String author) {

        return ResponseEntity.ok(
                new ApiResponseWrapper<>(200, "execute ok",
                        services.getBlueprintsByAuthor(author))
        );
    }

    @Operation(
            summary = "Retrieve a specific blueprint",
            description = "Returns a blueprint identified by its author and blueprint name."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Blueprint retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Blueprint not found")
    })
    @GetMapping("/{author}/{blueprintName}")
    public ResponseEntity<ApiResponseWrapper<Blueprint>> byAuthorAndName(
            @Parameter(description = "Author of the blueprint", example = "john")
            @PathVariable String author,

            @Parameter(description = "Name of the blueprint", example = "house")
            @PathVariable String blueprintName) {

        return ResponseEntity.ok(
                new ApiResponseWrapper<>(200, "execute ok",
                        services.getBlueprint(author, blueprintName))
        );
    }

    @Operation(
            summary = "Create a new blueprint",
            description = "Creates a new blueprint with its associated list of points."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Blueprint created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<Void>> add(
            @Valid @RequestBody NewBlueprintRequest req) {

        Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
        services.addNewBlueprint(bp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseWrapper<>(201, "Blueprint created successfully", null));
    }

    @Operation(
            summary = "Add a point to an existing blueprint",
            description = "Adds a new coordinate point to the specified blueprint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Point added successfully"),
            @ApiResponse(responseCode = "404", description = "Blueprint not found")
    })
    @PutMapping("/{author}/{blueprintName}/points")
    public ResponseEntity<ApiResponseWrapper<Void>> addPoint(
            @Parameter(description = "Author of the blueprint", example = "john")
            @PathVariable String author,

            @Parameter(description = "Name of the blueprint", example = "house")
            @PathVariable String blueprintName,

            @RequestBody Point p) {

        services.addPoint(author, blueprintName, p.x(), p.y());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponseWrapper<>(202, "Point added successfully", null));
    }

    public record NewBlueprintRequest(

            @Schema(description = "Author of the blueprint", example = "john")
            @NotBlank String author,

            @Schema(description = "Name of the blueprint", example = "house")
            @NotBlank String name,

            @Schema(description = "List of coordinate points that compose the blueprint")
            @Valid java.util.List<Point> points
    ) {}
}
