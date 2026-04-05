package com.alora.app.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.alora.app.model.Paciente;
import java.util.List;

@Dao
public interface PacienteDao {
    @Query("SELECT * FROM pacientes")
    List<Paciente> getAllPacientesLocales();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Paciente> pacientes);

    @Query("DELETE FROM pacientes")
    void deleteAll();
}