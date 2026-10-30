package ru.practicum.core.compilation.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@Table(name = "compilations")
@AllArgsConstructor
@NoArgsConstructor
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Builder.Default
    @Column(name = "pinned", nullable = false)
    private Boolean pinned = false;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilations_id")
    )
    @Column(name = "event_id", nullable = false)
    private List<Long> events = new ArrayList<>();
}
