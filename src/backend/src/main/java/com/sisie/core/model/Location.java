package com.sisie.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Repraesentiert einen physischen Lagerplatz im Warehouse.
 * Beispiel: "A-01-12" (Regal A, Ebene 1, Fach 12)
 */
@Entity
@Table(name="locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Eindeutiger Code des Lagerplatzes.
	 */
	@Column(unique = true, nullable = false)
	private String locationCode;
}
