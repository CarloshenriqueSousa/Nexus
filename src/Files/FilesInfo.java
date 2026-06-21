package Files;

public class FilesInfo {
    
    /**
     * Identifies the category of a file based on its name/extension.
     * 
     * @param nomeArquivo The file name.
     * @return String representation of the type: "3d_model", "pcb_design", "autocad", "imagem" or "documento".
     */
    public static String obterTipoArquivo(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return "documento";
        }
        
        String ext = obterExtensao(nomeArquivo).toLowerCase();
        return switch (ext) {
            case "obj", "stl", "fbx", "gltf", "glb" -> "3d_model";
            case "pcb", "brd", "sch", "kicad_pcb" -> "pcb_design";
            case "dwg", "dxf" -> "autocad";
            case "png", "jpg", "jpeg", "gif", "svg" -> "imagem";
            default -> "documento";
        };
    }

    /**
     * Extracts the extension of a file name.
     */
    public static String obterExtensao(String nomeArquivo) {
        if (nomeArquivo == null) {
            return "";
        }
        int ponto = nomeArquivo.lastIndexOf('.');
        if (ponto == -1) {
            return "";
        }
        return nomeArquivo.substring(ponto + 1);
    }

    /**
     * Formats bytes into a human readable size format.
     */
    public static String formatarTamanho(long bytes) {
        if (bytes <= 0) {
            return "0 Bytes";
        }
        final String[] unidades = new String[] { "Bytes", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), unidades[digitGroups]);
    }
}
