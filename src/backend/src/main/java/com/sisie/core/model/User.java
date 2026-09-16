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
 * Repraesentiert einen User im Warehouse.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Unique username
	 */
	@Column(nullable = false, unique = true)
	private String username;

	/**
	 * Hashed user password
	 */
	@Column(nullable = false)
	private String password;

	/**
	 * User role
	 */
	@Column(nullable = false)
	private Enum<Role> role;

}
