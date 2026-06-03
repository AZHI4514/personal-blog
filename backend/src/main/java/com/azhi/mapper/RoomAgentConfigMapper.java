package com.azhi.mapper;

import com.azhi.pojo.RoomAgentConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RoomAgentConfigMapper {

    @Select("SELECT id, config_key, api_url, api_key, model, vision_mode, mcp_enabled, mcp_endpoint, mcp_tool_allowlist, updated_by, update_time FROM room_agent_config WHERE config_key = #{configKey} LIMIT 1")
    RoomAgentConfig findByConfigKey(String configKey);

    @Update("UPDATE room_agent_config SET api_url = #{apiUrl}, api_key = #{apiKey}, model = #{model}, vision_mode = #{visionMode}, mcp_enabled = #{mcpEnabled}, mcp_endpoint = #{mcpEndpoint}, mcp_tool_allowlist = #{mcpToolAllowlist}, updated_by = #{updatedBy} WHERE config_key = #{configKey}")
    int updateByConfigKey(RoomAgentConfig config);

    @Insert("INSERT INTO room_agent_config (config_key, api_url, api_key, model, vision_mode, mcp_enabled, mcp_endpoint, mcp_tool_allowlist, updated_by) VALUES (#{configKey}, #{apiUrl}, #{apiKey}, #{model}, #{visionMode}, #{mcpEnabled}, #{mcpEndpoint}, #{mcpToolAllowlist}, #{updatedBy})")
    int insert(RoomAgentConfig config);
}
