package com.sisie.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sisie.core.model.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
	
	Optional<Supplier> findByEmail(String contactEmail);

}
