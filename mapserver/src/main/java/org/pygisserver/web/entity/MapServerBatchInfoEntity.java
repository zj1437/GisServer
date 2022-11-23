package org.pygisserver.web.entity;

import lombok.Data;
import org.pygisserver.common.ui.entity.MapServerBatchInfo;

import java.util.List;

/**
 * 批量发布
 *
 * @author 张亨特
 * @since 2021-10-23
 */
@Data
public class MapServerBatchInfoEntity {
    private List<MapServerBatchInfo> dataServer;

    private String groupLayerName;

    /**
     * 是否编组
     */
    private boolean editGroup;

    private String describe;

    private int openTiled;
}
