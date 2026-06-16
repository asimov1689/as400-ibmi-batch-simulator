package com.example.ibmi.dto;

public class ApiResponse<T> {

    private T data;
    private String ibmiConcept;

    public ApiResponse(T data, String ibmiConcept) {
        this.data = data;
        this.ibmiConcept = ibmiConcept;
    }

    public T getData()              { return data; }
    public String getIbmiConcept()  { return ibmiConcept; }
}
