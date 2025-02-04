    /*
     * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
     * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
     */
    package com.mycompany.javafxapplication1;

    /**
     *
     * @author ntu-user
     */
    public class Response {
        private String fileId;
        private String status;

        public Response(String fileId, String status) {
            this.fileId = fileId;
            this.status = status;
        }

        public String getFileId() {
            return fileId;
        }

        public String getStatus() {
            return status;
        }

        @Override
        public String toString() {
            return "Response{fileId='" + fileId + "', status='" + status + "'}";
        }
    }
