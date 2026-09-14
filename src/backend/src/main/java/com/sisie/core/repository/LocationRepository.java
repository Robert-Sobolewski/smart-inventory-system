package com.sisie.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sisie.core.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

	Optional<Location> findByLocationCode(String locationCode);
}
