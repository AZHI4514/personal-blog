package com.azhi.mapper;

import com.azhi.pojo.LifeCharacter;
import com.azhi.pojo.LifeEvent;
import com.azhi.pojo.LifeUser;
import com.azhi.pojo.LlmConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LifeMapper {

    // ==================== life_user ====================

    @Insert("INSERT INTO life_user (device_id) VALUES (#{deviceId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(LifeUser user);

    @Select("SELECT id, device_id, created_at FROM life_user WHERE device_id = #{deviceId}")
    LifeUser selectUserByDeviceId(String deviceId);

    // ==================== life_llm_config ====================

    @Insert("INSERT INTO life_llm_config (user_id, base_url, api_key, model_name, custom_prompt) VALUES (#{userId}, #{baseUrl}, #{apiKey}, #{modelName}, #{customPrompt})")
    int insertLlmConfig(LlmConfig config);

    @Update("UPDATE life_llm_config SET base_url = #{baseUrl}, api_key = #{apiKey}, model_name = #{modelName}, custom_prompt = #{customPrompt}, updated_at = CURRENT_TIMESTAMP WHERE user_id = #{userId}")
    int updateLlmConfig(LlmConfig config);

    @Select("SELECT id, user_id, base_url, api_key, model_name, custom_prompt, created_at, updated_at FROM life_llm_config WHERE user_id = #{userId}")
    LlmConfig selectLlmConfigByUserId(Long userId);

    @Delete("DELETE FROM life_llm_config WHERE user_id = #{userId}")
    int deleteLlmConfigByUserId(Long userId);

    // ==================== life_character ====================

    @Insert("INSERT INTO life_character (user_id, name, age, money, health, happiness, morality, knowledge, is_alive, generation) " +
            "VALUES (#{userId}, #{name}, #{age}, #{money}, #{health}, #{happiness}, #{morality}, #{knowledge}, #{isAlive}, #{generation})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCharacter(LifeCharacter character);

    @Select("SELECT id, user_id, name, age, money, health, happiness, morality, knowledge, is_alive, generation, created_at, updated_at " +
            "FROM life_character WHERE id = #{id}")
    LifeCharacter selectCharacterById(Long id);

    @Select("SELECT id, user_id, name, age, money, health, happiness, morality, knowledge, is_alive, generation, created_at, updated_at " +
            "FROM life_character WHERE user_id = #{userId} AND is_alive = TRUE ORDER BY updated_at DESC LIMIT 1")
    LifeCharacter selectAliveCharacterByUserId(Long userId);

    @Update("UPDATE life_character SET age = #{age}, money = #{money}, health = #{health}, happiness = #{happiness}, " +
            "morality = #{morality}, knowledge = #{knowledge}, is_alive = #{isAlive}, generation = #{generation}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateCharacter(LifeCharacter character);

    @Update("UPDATE life_character SET is_alive = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int killCharacter(Long id);

    @Delete("DELETE FROM life_character WHERE id = #{id}")
    int deleteCharacter(Long id);

    @Delete("DELETE FROM life_character WHERE user_id = #{userId}")
    int deleteCharactersByUserId(Long userId);

    // ==================== life_event ====================

    @Insert("INSERT INTO life_event (character_id, age, description, choice_made, effects) " +
            "VALUES (#{characterId}, #{age}, #{description}, #{choiceMade}, #{effects})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEvent(LifeEvent event);

    @Select("SELECT id, character_id, age, description, choice_made, effects, created_at " +
            "FROM life_event WHERE character_id = #{characterId} ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<LifeEvent> selectEventsByCharacterId(@Param("characterId") Long characterId,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    @Select("SELECT COUNT(*) FROM life_event WHERE character_id = #{characterId}")
    int countEventsByCharacterId(Long characterId);

    @Delete("DELETE FROM life_event WHERE character_id = #{characterId}")
    int deleteEventsByCharacterId(Long characterId);

    @Delete("DELETE FROM life_event WHERE character_id IN (SELECT id FROM life_character WHERE user_id = #{userId})")
    int deleteEventsByUserId(Long userId);

    // ==================== life_user ====================

    @Delete("DELETE FROM life_user WHERE id = #{id}")
    int deleteUserById(Long id);

    // ==================== DDL（仅首次执行） ====================

    @Update("CREATE TABLE IF NOT EXISTS life_user (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "device_id VARCHAR(128) NOT NULL UNIQUE, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
    void createLifeUserTable();

    @Update("CREATE TABLE IF NOT EXISTS life_llm_config (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "user_id BIGINT NOT NULL UNIQUE, " +
            "base_url VARCHAR(255) NOT NULL, " +
            "api_key VARCHAR(512) NOT NULL, " +
            "model_name VARCHAR(64) DEFAULT 'gpt-3.5-turbo', " +
            "custom_prompt TEXT, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (user_id) REFERENCES life_user(id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
    void createLlmConfigTable();

    @Update("CREATE TABLE IF NOT EXISTS life_character (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "user_id BIGINT NOT NULL, " +
            "name VARCHAR(64) NOT NULL, " +
            "age INT DEFAULT 0, " +
            "money INT DEFAULT 100, " +
            "health INT DEFAULT 80, " +
            "happiness INT DEFAULT 60, " +
            "morality INT DEFAULT 50, " +
            "knowledge INT DEFAULT 30, " +
            "is_alive BOOLEAN DEFAULT TRUE, " +
            "generation INT DEFAULT 1, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (user_id) REFERENCES life_user(id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
    void createLifeCharacterTable();

    /** 迁移：为已存在的 life_llm_config 表添加 custom_prompt 列 */
    @Update("ALTER TABLE life_llm_config ADD COLUMN custom_prompt TEXT")
    void addCustomPromptColumn();

    @Update("CREATE TABLE IF NOT EXISTS life_event (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "character_id BIGINT NOT NULL, " +
            "age INT NOT NULL, " +
            "description TEXT NOT NULL, " +
            "choice_made VARCHAR(255), " +
            "effects JSON, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (character_id) REFERENCES life_character(id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
    void createLifeEventTable();
}
