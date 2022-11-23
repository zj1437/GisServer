package org.pygisserver.common.entity;

/**
 * 响应结果状态枚举
 * 
 * @author 
 * @since 2017-12-14
 */
public enum ReponseStatus
{
    SUCCESS(200,"success"),
    
    WARN(300,"warn"),
    
    NOT_FOUND(404,"not found"),
    
    ERROR(500,"error");
    
    ReponseStatus(int statusCode,String statusInfo)
    {
        this.code = statusCode;
        this.result = statusInfo;
    }
    
    private int code;
    
    private String result;

    public int getCode()
    {
        return code;
    }

    public String getResult()
    {
        return result;
    }
    
    public String getResultByCode(int code)
    {
        for (ReponseStatus status : ReponseStatus.values())
        {
            if (status.getCode() == code)
            {
                return status.getResult();
            }
        }
        return null;
    }
    
    @Override
    public String toString()
    {
        return "{\"status\":" + this.code + "," + "\"statusInfo\":\"" + this.result+ "\"}";
    }
}
