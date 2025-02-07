package com.example.QRAPI.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import java.util.UUID;
import com.example.QRAPI.model.*;

public interface HistoryRepository extends CassandraRepository<History, UUID> {
}
