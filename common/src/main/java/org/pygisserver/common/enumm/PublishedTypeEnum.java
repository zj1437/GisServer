package org.pygisserver.common.enumm;

public enum PublishedTypeEnum {
	VECTOR {
        public Integer getCode() {
            return 0;
        }
    },
    RASTER {
        public Integer getCode() {
            return 1;
        }
    },
    WMS {
        public Integer getCode() {
            return 3;
        }
    },
    GROUP {
        public Integer getCode() {
            return 4;
        }
    },
	TILED {
	        
        public Integer getCode() {
            return 6;
        }
    },
	ONLINE {
        
        public Integer getCode() {
            return 7;
        }
    };

    public abstract Integer getCode();
}
