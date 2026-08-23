package br.com.lumiflow.service;
import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.usuario.*;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.repository.UsuarioRepository;
import br.com.lumiflow.validation.LoginValidator;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
@Service @AllArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository; private final UsuarioMapper usuarioMapper; private final PasswordEncoder passwordEncoder; private final NivelAcessoService nivelAcessoService; private final SetorService setorService;
    public void validarLogin(String login) { if (!LoginValidator.isValidLogin(login)) throw new BusinessException(AppMessages.ERROR_USER_LOGIN_INVALID); if (usuarioRepository.findByLogin(login.trim()).isPresent()) throw new BusinessException(AppMessages.ERROR_USER_ALREADY_EXISTS); }
    public Usuario validarUsuarioPorId(Long id) { return usuarioRepository.findById(id).orElseThrow(() -> new BusinessException(AppMessages.ERROR_USER_NOTFOUND)); }
    @Transactional public void novoUsuario(UsuarioDTO dto) { validarLogin(dto.login()); validarSenha(dto.senha()); Usuario usuario = usuarioMapper.toEntity(dto); usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(dto.nivelAcessoId())); usuario.setSetor(dto.setorId() == null ? null : setorService.buscarSetorPorId(dto.setorId())); usuario.setSenha(passwordEncoder.encode(dto.senha())); usuarioRepository.save(usuario); }
    @Transactional(readOnly = true) public List<UsuarioListaDTO> listarTodos() { return usuarioMapper.toListDTO(usuarioRepository.findAll()); }
    @Transactional public void excluirUsuario(Long id) { usuarioRepository.delete(validarUsuarioPorId(id)); }
    @Transactional public void editarUsuario(Long id, UsuarioEdicaoDTO dto) { Usuario usuario = validarUsuarioPorId(id); if (!LoginValidator.isValidLogin(dto.login())) throw new BusinessException(AppMessages.ERROR_USER_LOGIN_INVALID); usuarioRepository.findByLogin(dto.login()).filter(outro -> !outro.getId().equals(id)).ifPresent(outro -> { throw new BusinessException(AppMessages.ERROR_USER_ALREADY_EXISTS); }); usuario.setNome(dto.nome().toUpperCase(Locale.ROOT)); usuario.setLogin(dto.login().trim()); usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(dto.nivelAcessoId())); usuario.setSetor(dto.setorId() == null ? null : setorService.buscarSetorPorId(dto.setorId())); if (dto.senha() != null && !dto.senha().isBlank()) { validarSenha(dto.senha()); usuario.setSenha(passwordEncoder.encode(dto.senha())); } usuarioRepository.save(usuario); }
    public UsuarioLogadoDTO obterUsuarioLogado() { Usuario usuario = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); return new UsuarioLogadoDTO(usuario.getNome(), usuario.getNivelAcesso().getDescricao().name()); }
    private void validarSenha(String senha) { if (!LoginValidator.isValidPassword(senha)) throw new BusinessException(AppMessages.ERROR_USER_PASSWORD_WEAK); }
}
