package org.pygisserver.pytc.controller;

import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.pytc.service.RenderTileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Description: 瓦片渲染处理 
 * @author:  
 * @date: 2021-03-26
 */
//@RestController
@RequestMapping("/seed")
public class RenderTileController {

	@Autowired
	private RenderTileService renderTileService;
	
	/**
	 * 手动开始切图
	 * @param seedParam 切图参数
	 */
	@PostMapping("/task")
	public void seedTask(@RequestBody SeedTaskInstance seedTaskInstance) {
		renderTileService.seedTaskByParam(seedTaskInstance);
	}
	
	/**
	 * 激活当前任务
	 * @param seedParam 切图参数
	 */
	@GetMapping("/active/{layer}")
	public void activeSeedTask(@PathVariable(name="layer") String layer) {
		renderTileService.activeSeedTask(layer);
	}
	
	/**
	 * 暂停当前任务
	 * @param seedParam 切图参数
	 */
	@GetMapping("/stop/{layer}")
	public void stopSeedTask(@PathVariable(name="layer") String layer) {
		renderTileService.stopSeedTask(layer);
	}
	
}
